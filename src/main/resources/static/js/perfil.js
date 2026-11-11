const formularioPerfil = document.querySelector('[data-autoguardado]');

if (formularioPerfil) {
  const campoNombre = formularioPerfil.querySelector('#nombre');
  const campoAvatar = formularioPerfil.querySelector('#avatar');
  let temporizador;

  campoNombre.addEventListener('input', () => {
    clearTimeout(temporizador);
    temporizador = setTimeout(() => formularioPerfil.requestSubmit(), 800);
  });

  campoAvatar.addEventListener('change', () => {
    clearTimeout(temporizador);
    formularioPerfil.requestSubmit();
  });
}
