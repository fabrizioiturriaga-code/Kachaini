document.querySelectorAll('[data-password]').forEach(button => {
  button.addEventListener('click', () => {
    const campo = document.getElementById(button.dataset.password);
    const mostrar = campo.type === 'password';
    campo.type = mostrar ? 'text' : 'password';
    button.textContent = mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña';
    button.setAttribute('aria-pressed', String(mostrar));
  });
});

const selectorRol = document.getElementById('rol');

function actualizarCamposPorRol() {
  for (const [id, rol] of [['estudiante', 'ESTUDIANTE'], ['docente', 'DOCENTE']]) {
    const grupo = document.getElementById(id);
    const corresponde = selectorRol.value === rol;
    grupo.disabled = !corresponde;
    grupo.hidden = !corresponde;
    grupo.querySelectorAll('input, select').forEach(campo => {
      campo.required = corresponde;
    });
  }
}

if (selectorRol) {
  selectorRol.addEventListener('change', actualizarCamposPorRol);
  actualizarCamposPorRol();
}

const primerError = Array.from(document.querySelectorAll('[aria-invalid="true"]'))
  .find(campo => !campo.matches(':disabled') && !campo.closest('[hidden]'));
const resumenErrores = document.getElementById('resumen-errores') || document.getElementById('error-login');

if (primerError) {
  primerError.focus();
} else if (resumenErrores) {
  resumenErrores.focus();
}
