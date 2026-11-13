package kanichi.controller;

import java.nio.charset.StandardCharsets;
import kanichi.dto.ReporteSalon;
import kanichi.service.ProgresoDocenteService;
import kanichi.service.ReporteSalonService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/docente/reportes")
public class ReporteController {
  private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final ReporteSalonService reporteSalonService;

  public ReporteController(ReporteSalonService reporteSalonService) {
    this.reporteSalonService = reporteSalonService;
  }

  @GetMapping
  public String reportes(
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion,
      Model model) {
    String seccionValida = ProgresoDocenteService.normalizarSeccion(seccion);
    model.addAttribute("grados", ProgresoDocenteService.GRADOS);
    model.addAttribute("secciones", ProgresoDocenteService.SECCIONES);
    model.addAttribute("seccion", seccionValida);
    if (grado == null) {
      return "docente/reportes";
    }
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      model.addAttribute("error", "Selecciona un grado de 1 a 5.");
      return "docente/reportes";
    }
    model.addAttribute("grado", grado);
    model.addAttribute("reporte", reporteSalonService.generar(grado, seccionValida));
    model.addAttribute("minimoIntentos", ReporteSalonService.MINIMO_INTENTOS);
    model.addAttribute("aciertosMinimos", ReporteSalonService.ACIERTOS_MINIMOS);
    return "docente/reportes";
  }

  @GetMapping("/exportar")
  public ResponseEntity<byte[]> exportar(
      @RequestParam(required = false) Integer grado,
      @RequestParam(required = false) String seccion) {
    if (!ProgresoDocenteService.esGradoValido(grado)) {
      return ResponseEntity.badRequest().build();
    }
    ReporteSalon reporte =
        reporteSalonService.generar(grado, ProgresoDocenteService.normalizarSeccion(seccion));
    return ResponseEntity.ok()
        .contentType(CSV)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(reporteSalonService.nombreArchivo(reporte))
                .build()
                .toString())
        .body(reporteSalonService.exportarCsv(reporte).getBytes(StandardCharsets.UTF_8));
  }
}
