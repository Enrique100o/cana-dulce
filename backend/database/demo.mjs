// Material ficticio de demostración. La docente puede crear sus propias actividades.
export const demoReadings = [
  {
    titulo: 'El árbol y los vecinos', grado: 3, dificultad: 'BAJA', categoria: 'Comunidad',
    contenido: 'En el patio de la escuela había un árbol sin agua. Ana invitó a sus vecinos a cuidarlo. Entre todos lo regaron cada mañana. Al final de la semana, sus hojas volvieron a crecer.',
    preguntas: [
      ['¿Quién invitó a los vecinos a cuidar el árbol?', 'LITERAL', ['Ana','El director','Un visitante']],
      ['¿Cuál es la idea principal de la historia?', 'IDEA_PRINCIPAL', ['La comunidad puede cuidar la naturaleza trabajando unida.','La escuela cerró durante una semana.','Ana compró un árbol nuevo.']],
      ['¿Qué significa «invitó» en esta historia?', 'VOCABULARIO', ['Les propuso participar.','Les pidió que se fueran.','Les quitó el agua.']],
      ['¿Qué secuencia corresponde a la historia?', 'SECUENCIA', ['Árbol sin agua, invitación de Ana, riego y nuevas hojas.','Nuevas hojas, riego, invitación y árbol sin agua.','Riego, nuevas hojas y compra del árbol.']]
    ]
  },
  {
    titulo: 'La semilla del patio', grado: 3, dificultad: 'BAJA', categoria: 'Naturaleza',
    contenido: 'Lucía encontró una semilla en el patio de su escuela. La sembró en una maceta y la regó cada día. Después de una semana apareció un brote. Lucía aprendió que las plantas necesitan cuidado y paciencia.',
    preguntas: [
      ['¿Qué encontró Lucía en el patio?', 'LITERAL', ['Una semilla.','Un libro.','Una pelota.']],
      ['¿Por qué Lucía regó la semilla cada día?', 'INFERENCIAL', ['Para ayudarla a crecer.','Para esconder la maceta.','Para enfriar un libro.']],
      ['¿Qué enseña principalmente la historia?', 'IDEA_PRINCIPAL', ['Las plantas necesitan cuidado y paciencia.','Las plantas crecen sin agua.','Todas las semillas son juguetes.']],
      ['¿Qué significa «brote» en el texto?', 'VOCABULARIO', ['El inicio de una planta que crece.','Un cuaderno nuevo.','Un agujero en el patio.']]
    ]
  },
  {
    titulo: 'El libro compartido', grado: 3, dificultad: 'MEDIA', categoria: 'Amistad',
    contenido: 'Mateo llevó un libro de animales a la escuela. Sara quería leerlo, pero Mateo aún no había terminado. Acordaron leer una página por turnos. Al final, los dos descubrieron nuevos animales y decidieron cuidar el libro juntos.',
    preguntas: [
      ['¿De qué trataba el libro de Mateo?', 'LITERAL', ['De animales.','De automóviles.','De recetas.']],
      ['¿Cómo resolvieron el deseo de leer el mismo libro?', 'LITERAL', ['Leyeron una página por turnos.','Rompieron las páginas.','Dejaron de leer para siempre.']],
      ['¿Qué podemos aprender de Mateo y Sara?', 'INFERENCIAL', ['Compartir permite disfrutar y aprender juntos.','Los libros solo pueden leerse solos.','Cuidar un libro no importa.']],
      ['¿Cuál es la secuencia correcta?', 'SECUENCIA', ['Mateo llevó el libro, acordaron turnos y descubrieron animales.','Descubrieron animales, rompieron el libro y fueron a casa.','Sara cocinó, Mateo jugó y el libro desapareció.']]
    ]
  },
  {
    titulo: 'El puente del arroyo', grado: 4, dificultad: 'MEDIA', categoria: 'Comunidad',
    contenido: 'Después de una fuerte lluvia, el pequeño puente del arroyo quedó dañado. Las familias del pueblo organizaron una jornada para repararlo. Un grupo reunió materiales y otro limpió el camino. Cuando terminaron, todos pudieron cruzar de nuevo con cuidado. El trabajo organizado ayudó a resolver una necesidad común.',
    preguntas: [
      ['¿Qué dañó el puente del arroyo?', 'LITERAL', ['Una fuerte lluvia.','Un incendio.','Una fiesta.']],
      ['¿Por qué las familias se organizaron en grupos?', 'INFERENCIAL', ['Para repartir tareas y reparar el puente.','Para impedir que alguien cruzara después.','Para cambiar el nombre del arroyo.']],
      ['¿Cuál es la idea principal del texto?', 'IDEA_PRINCIPAL', ['El trabajo organizado puede resolver una necesidad común.','Los caminos deben mantenerse sucios.','Las lluvias nunca dañan los puentes.']],
      ['¿Qué significa «jornada» en este contexto?', 'VOCABULARIO', ['Un tiempo dedicado a realizar una actividad.','Una herramienta de metal.','Un animal del arroyo.']]
    ]
  }
];
export function demoQuestion(item, order) {
  return { enunciado: item[0], tipo: item[1], orden: order, opciones: item[2].map((texto,index) => ({ texto, orden: index+1, esCorrecta: index===0 })) };
}
