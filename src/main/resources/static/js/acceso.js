document.addEventListener('click', evento => {
  const ojo = evento.target.closest('.ojo');
  if (ojo) {
    const campo = document.getElementById(ojo.dataset.campo);
    const mostrar = campo.type === 'password';
    campo.type = mostrar ? 'text' : 'password';
    ojo.setAttribute('aria-pressed', String(mostrar));
    ojo.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
  }
});

const opcionesRol = document.querySelectorAll('input[name="rol"]');

function actualizarCamposPorRolElegido() {
  const elegido = document.querySelector('input[name="rol"]:checked');
  for (const [id, rol] of [['estudiante', 'ESTUDIANTE'], ['docente', 'DOCENTE']]) {
    const grupo = document.getElementById(id);
    if (!grupo) {
      continue;
    }
    const corresponde = elegido !== null && elegido.value === rol;
    grupo.disabled = !corresponde;
    grupo.hidden = !corresponde;
    grupo.querySelectorAll('input, select').forEach(campo => {
      campo.required = corresponde;
    });
  }
}

if (opcionesRol.length) {
  opcionesRol.forEach(opcion => opcion.addEventListener('change', actualizarCamposPorRolElegido));
  actualizarCamposPorRolElegido();
}
