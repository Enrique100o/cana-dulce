package com.canadulce.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

class ApiException(val status: Int, val code: String, message: String, val existingAttempt: Int? = null) : Exception(message)

/** HTTP/JSON real; las claves correctas y el puntaje los determina la API. */
class ApiClient(var baseUrl: String, var token: String = "") {
    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank()) throw IOException("Configura la conexión de aula para continuar.")
            val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = method
                connection.connectTimeout = 10000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")
                if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer " + token)
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = if (raw.isBlank()) JSONObject() else try { JSONObject(raw) } catch (_: Exception) {
                    throw IOException("La conexión no devolvió datos válidos. Comprueba la dirección de aula.")
                }
                if (status !in 200..299) {
                    val error = json.optJSONObject("error")
                    val details = error?.optJSONArray("detalles")
                    val explanation = (0 until (details?.length() ?: 0)).map {
                        details!!.getJSONObject(it).optString("mensaje")
                    }.filter { it.isNotBlank() }.joinToString("\n")
                    val message = error?.optString("mensaje")?.takeIf { it.isNotBlank() } ?: "No se pudo completar la solicitud."
                    throw ApiException(status, error?.optString("codigo").orEmpty(),
                        message + if (explanation.isNotBlank()) "\n" + explanation else "",
                        error?.takeIf { it.has("idIntentoExistente") }?.getInt("idIntentoExistente"))
                }
                json
            } finally { connection.disconnect() }
        }

    suspend fun loginStudent(alias: String, code: String): Session =
        session(request("POST", "/sesiones/estudiante", obj("alias" to alias.trim(), "codigoAcceso" to code)))
    suspend fun loginTeacher(user: String, password: String): Session =
        session(request("POST", "/sesiones/docente", obj("usuario" to user, "contrasena" to password)))
    suspend fun register(alias: String, grade: Int, code: String): Student =
        student(request("POST", "/estudiantes", obj("alias" to alias.trim(), "grado" to grade, "codigoAcceso" to code)))
    private fun filters(query: String, grade: Int?, status: String? = null): String {
        val values = mutableListOf<String>()
        if (query.trim().isNotEmpty()) values.add("q=" + URLEncoder.encode(query.trim(), "UTF-8"))
        if (grade != null) values.add("grado=" + grade)
        if (status != null) values.add("estado=" + status)
        return if (values.isEmpty()) "" else "?" + values.joinToString("&")
    }
    suspend fun readings(query: String = "", grade: Int? = null, status: String? = null): List<Reading> =
        request("GET", "/lecturas" + filters(query, grade, status)).array("datos", ::reading)
    suspend fun reading(id: Int): Reading = reading(request("GET", "/lecturas/" + id))
    suspend fun questions(id: Int, teacher: Boolean = false): List<Question> =
        request("GET", (if (teacher) "/docente" else "") + "/lecturas/" + id + "/preguntas").array("datos", ::question)
    suspend fun start(id: Int): Attempt = attempt(request("POST", "/intentos", obj("idLectura" to id)))
    suspend fun attempt(id: Int): Attempt = attempt(request("GET", "/intentos/" + id))
    suspend fun attempts(studentId: Int? = null): List<Attempt> =
        request("GET", "/intentos" + if (studentId == null) "" else "?estudianteId=" + studentId).array("datos", ::attempt)
    suspend fun finish(id: Int, answers: Map<Int, Int>): Attempt {
        val array = JSONArray()
        answers.toSortedMap().forEach { (q, o) -> array.put(obj("idPregunta" to q, "idOpcion" to o)) }
        return attempt(request("POST", "/intentos/" + id + "/finalizar", obj("respuestas" to array)))
    }
    suspend fun progress(studentId: Int): Progress {
        val j = request("GET", "/estudiantes/" + studentId + "/progreso")
        return Progress(j.getString("alias"), j.getInt("grado"), j.getInt("intentosCompletados"),
            j.getInt("lecturasCompletadas"), j.number("puntajePromedio"), j.number("mejorPuntaje"),
            j.getDouble("puntosAcumulados"), j.getInt("estrellasAcumuladas"),
            j.array("insignias") { Badge(it.getString("codigo"), it.getString("nombre"), it.getBoolean("obtenida")) })
    }
    suspend fun students(query: String = "", grade: Int? = null): List<Student> =
        request("GET", "/estudiantes" + filters(query, grade)).array("datos", ::student)
    suspend fun saveReading(d: ReadingDraft): Reading {
        val body = obj("titulo" to d.title.trim(), "contenido" to d.content.trim(), "grado" to d.grade,
            "dificultad" to d.difficulty, "categoria" to d.category.trim().ifEmpty { null })
        if (d.id != null) body.put("estado", d.status)
        return reading(request(if (d.id == null) "POST" else "PUT",
            "/lecturas" + if (d.id == null) "" else "/" + d.id, body))
    }
    suspend fun setReadingState(r: Reading, active: Boolean): Reading = saveReading(
        ReadingDraft(r.id, r.title, r.content, r.grade, r.difficulty, r.category, if (active) "ACTIVA" else "INACTIVA"))
    suspend fun deactivateReading(id: Int) { request("DELETE", "/lecturas/" + id) }
    suspend fun saveQuestion(readingId: Int, d: QuestionDraft): Question {
        val choices = JSONArray()
        d.options.forEachIndexed { i, value -> choices.put(obj("texto" to value.trim(), "orden" to i + 1, "esCorrecta" to (i == d.correctIndex))) }
        val body = obj("enunciado" to d.text.trim(), "tipo" to d.type, "orden" to d.order.toInt(), "opciones" to choices)
        if (d.id != null) body.put("estado", d.status)
        return question(request(if (d.id == null) "POST" else "PUT",
            if (d.id == null) "/lecturas/" + readingId + "/preguntas" else "/preguntas/" + d.id, body))
    }
    suspend fun deactivateQuestion(id: Int) { request("DELETE", "/preguntas/" + id) }

    companion object {
        fun validBaseUrl(value: String, debug: Boolean): Boolean = try {
            val uri = URI(value.trim())
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            (scheme == "https" || (debug && scheme == "http" && host in listOf("10.0.2.2", "127.0.0.1", "localhost"))) &&
                !host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null &&
                uri.path.trimEnd('/') == "/api" && (uri.port == -1 || uri.port in 1..65535)
        } catch (_: Exception) { false }
        fun obj(vararg pairs: Pair<String, Any?>): JSONObject = JSONObject().apply {
            pairs.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
        }
        private fun student(j: JSONObject) = Student(j.getInt("idEstudiante"), j.getString("alias"), j.getInt("grado"), j.getString("estado"))
        private fun session(j: JSONObject) = Session(j.getString("token"), j.getString("rol"), j.optJSONObject("estudiante")?.let(::student))
        private fun reading(j: JSONObject) = Reading(j.getInt("idLectura"), j.getString("titulo"), j.optString("contenido"),
            j.getInt("grado"), j.getString("dificultad"), if (j.isNull("categoria")) "" else j.optString("categoria"),
            j.getString("estado"), j.getInt("totalPreguntasActivas"), j.getBoolean("bloqueadaParaEdicion"))
        private fun question(j: JSONObject) = Question(j.getInt("idPregunta"), j.getInt("idLectura"),
            j.getString("enunciado"), j.getString("tipo"), j.getInt("orden"), j.optString("estado", "ACTIVA"),
            j.array("opciones") { Option(it.getInt("idOpcion"), it.getString("texto"), it.getInt("orden"), it.optBoolean("esCorrecta")) })
        private fun attempt(j: JSONObject) = Attempt(j.getInt("idIntento"), j.getInt("idEstudiante"),
            j.getInt("idLectura"), j.getString("tituloLectura"), j.getString("fechaInicio"),
            if (j.isNull("fechaFinalizacion")) null else j.getString("fechaFinalizacion"),
            if (j.isNull("cantidadCorrectas")) null else j.getInt("cantidadCorrectas"), j.getInt("totalPreguntas"),
            j.number("puntaje"), if (j.isNull("estrellas")) null else j.getInt("estrellas"), j.getString("estado"),
            j.array("respuestas") { Answer(it.getInt("idPregunta"), it.getString("enunciado"), it.getString("textoOpcionElegida"), it.getBoolean("esCorrecta")) })
        private fun JSONObject.number(key: String): Double? = if (isNull(key)) null else getDouble(key)
        private fun <T> JSONObject.array(key: String, convert: (JSONObject) -> T): List<T> {
            val array = optJSONArray(key) ?: return emptyList()
            return (0 until array.length()).map { convert(array.getJSONObject(it)) }
        }
    }
}
