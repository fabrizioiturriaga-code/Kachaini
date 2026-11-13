package kanichi.dto;

import kanichi.model.RetoDocente;

public record RetoPublicado(RetoDocente reto, long notificados, long leidos) {}