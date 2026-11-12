const botonEscuchar = document.querySelector('[data-escuchar]');
const texto = document.querySelector('[data-texto]');

if (botonEscuchar && texto && 'speechSynthesis' in window) {
  const voz = window.speechSynthesis;
  const textoBoton = botonEscuchar.textContent;
  botonEscuchar.hidden = false;

  const terminar = () => {
    botonEscuchar.textContent = textoBoton;
    botonEscuchar.setAttribute('aria-pressed', 'false');
  };

  botonEscuchar.addEventListener('click', () => {
    if (voz.speaking) {
      voz.cancel();
      terminar();
      return;
    }
    const lectura = new SpeechSynthesisUtterance(texto.innerText);
    lectura.lang = 'es-PE';
    lectura.rate = 0.95;
    lectura.addEventListener('end', terminar);
    lectura.addEventListener('error', terminar);
    voz.speak(lectura);
    botonEscuchar.textContent = '⏹ Detener';
    botonEscuchar.setAttribute('aria-pressed', 'true');
  });

  window.addEventListener('pagehide', () => voz.cancel());
}

document.querySelector('[data-enfocar-resultado]')?.focus();
