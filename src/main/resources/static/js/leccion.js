const formulario = document.querySelector('[data-opcion-multiple]');

if (formulario && typeof formulario.requestSubmit === 'function') {
  let enviando = false;
  formulario.querySelector('[data-responder]').hidden = true;

  formulario.addEventListener('change', evento => {
    if (evento.target.matches('input[name="respuesta"]') && !enviando) {
      formulario.requestSubmit();
    }
  });

  formulario.addEventListener('submit', evento => {
    if (enviando) {
      evento.preventDefault();
      return;
    }
    enviando = true;
    formulario.setAttribute('aria-busy', 'true');
  });

  window.addEventListener('pageshow', () => {
    enviando = false;
    formulario.removeAttribute('aria-busy');
  });
}

document.querySelector('[data-enfocar-resultado]')?.focus();
