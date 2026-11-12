const formulario = document.querySelector('[data-arrastre]');

if (formulario) {
  const banco = formulario.querySelector('[data-banco]');
  const ayuda = formulario.querySelector('[data-ayuda]');
  const aviso = formulario.querySelector('[data-aviso]');
  const destinos = [...formulario.querySelectorAll('[data-destino]')];
  const fichas = new Map();
  let seleccionada = null;

  const zonaDe = destino => destino.querySelector('[data-zona]');
  const selectDe = destino => destino.querySelector('select');

  const sincronizar = () => {
    destinos.forEach(destino => {
      const ficha = zonaDe(destino).querySelector('.arrastre__ficha');
      selectDe(destino).value = ficha ? ficha.dataset.valor : '';
    });
    aviso.textContent = '';
  };

  const seleccionar = ficha => {
    if (seleccionada) {
      seleccionada.setAttribute('aria-pressed', 'false');
    }
    seleccionada = ficha === seleccionada ? null : ficha;
    if (seleccionada) {
      seleccionada.setAttribute('aria-pressed', 'true');
    }
  };

  const ubicar = (ficha, destino) => {
    if (destino) {
      const zona = zonaDe(destino);
      const ocupante = zona.querySelector('.arrastre__ficha');
      if (ocupante && ocupante !== ficha) {
        banco.append(ocupante);
      }
      zona.append(ficha);
      destino.classList.remove('arrastre__destino--correcto', 'arrastre__destino--incorrecto');
    } else {
      banco.append(ficha);
    }
    if (seleccionada) {
      seleccionar(seleccionada);
    }
    sincronizar();
  };

  [...selectDe(destinos[0]).options]
    .filter(opcion => opcion.value !== '')
    .forEach(opcion => {
      const ficha = document.createElement('button');
      ficha.type = 'button';
      ficha.className = 'arrastre__ficha';
      ficha.textContent = opcion.textContent;
      ficha.dataset.valor = opcion.value;
      ficha.draggable = true;
      ficha.setAttribute('aria-pressed', 'false');
      ficha.addEventListener('click', evento => {
        evento.stopPropagation();
        seleccionar(ficha);
      });
      ficha.addEventListener('dragstart', evento => {
        evento.dataTransfer.setData('text/plain', ficha.dataset.valor);
        evento.dataTransfer.effectAllowed = 'move';
        ficha.classList.add('arrastre__ficha--arrastrando');
      });
      ficha.addEventListener('dragend', () => ficha.classList.remove('arrastre__ficha--arrastrando'));
      fichas.set(opcion.value, ficha);
      banco.append(ficha);
    });

  const permitirSoltar = (elemento, destino) => {
    elemento.addEventListener('dragover', evento => {
      evento.preventDefault();
      elemento.classList.add('arrastre--encima');
    });
    elemento.addEventListener('dragleave', () => elemento.classList.remove('arrastre--encima'));
    elemento.addEventListener('drop', evento => {
      evento.preventDefault();
      elemento.classList.remove('arrastre--encima');
      const ficha = fichas.get(evento.dataTransfer.getData('text/plain'));
      if (ficha) {
        ubicar(ficha, destino);
      }
    });
  };

  destinos.forEach(destino => {
    const zona = zonaDe(destino);
    const select = selectDe(destino);
    select.hidden = true;
    select.required = false;
    destino.querySelector('label').removeAttribute('for');
    zona.hidden = false;

    const ficha = fichas.get(select.value);
    if (ficha) {
      zona.append(ficha);
    }

    zona.addEventListener('click', () => {
      if (seleccionada) {
        ubicar(seleccionada, destino);
      }
    });
    zona.addEventListener('keydown', evento => {
      if ((evento.key === 'Enter' || evento.key === ' ') && seleccionada) {
        evento.preventDefault();
        ubicar(seleccionada, destino);
      }
    });
    permitirSoltar(zona, destino);
  });

  banco.addEventListener('click', () => {
    if (seleccionada) {
      ubicar(seleccionada, null);
    }
  });
  permitirSoltar(banco, null);

  banco.hidden = false;
  ayuda.hidden = false;

  formulario.addEventListener('submit', evento => {
    if (destinos.some(destino => selectDe(destino).value === '')) {
      evento.preventDefault();
      aviso.textContent = 'Ubica todos los elementos antes de verificar.';
    }
  });
}

document.querySelector('[data-enfocar-resultado]')?.focus();
