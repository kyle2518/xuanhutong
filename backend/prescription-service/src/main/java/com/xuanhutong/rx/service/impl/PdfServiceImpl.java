package com.xuanhutong.rx.service.impl;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.xuanhutong.rx.entity.*;
import com.xuanhutong.common.entity.User;
import com.xuanhutong.common.exception.BusinessException;
import com.xuanhutong.common.exception.ErrorCode;
import com.xuanhutong.rx.service.PdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j @Service
public class PdfServiceImpl implements PdfService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");

    private PdfFont loadFont() {
        try { return PdfFontFactory.createFont("fonts/NotoSansCJKsc-Regular.ttf", PdfEncodings.IDENTITY_H); }
        catch (Exception e) {
            try { return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED); }
            catch (Exception ex) { throw new BusinessException(ErrorCode.PDF_GENERATION_FAILED); }
        }
    }

    public byte[] generatePrescriptionPdf(Prescription rx, List<PrescriptionItem> items, User doctor, String patientName, String sig) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc); PdfFont font = loadFont();
            doc.add(new Paragraph(doctor.getClinicName() != null ? doctor.getClinicName() : "悬壶通中医诊所").setFont(font).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setBold());
            doc.add(new Paragraph("中药处方笺").setFont(font).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("病人：" + patientName).setFont(font).setFontSize(9));
            if (rx.getDiagnosis() != null) doc.add(new Paragraph("诊断：" + rx.getDiagnosis()).setFont(font).setFontSize(9));
            Table table = new Table(UnitValue.createPercentArray(new float[]{5, 30, 15, 20, 30}));
            table.setWidth(UnitValue.createPercentValue(100));
            for (String h : new String[]{"序号","药材名称","剂量(g)","特殊用法","备注"})
                table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));
            int idx = 1;
            for (PrescriptionItem item : items) {
                table.addCell(new Cell().add(new Paragraph(String.valueOf(idx++)).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(item.getHerbName()).setFont(font).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph(item.getDosageGrams().stripTrailingZeros().toPlainString()).setFont(font).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(item.getNotes() != null ? item.getNotes() : "").setFont(font).setFontSize(9)));
                table.addCell(new Cell().add(new Paragraph("").setFont(font).setFontSize(9)));
            }
            doc.add(table);
            doc.add(new Paragraph("剂数：" + rx.getTotalDoses() + "剂").setFont(font).setFontSize(9));
            if (rx.getNotes() != null && !rx.getNotes().isEmpty()) doc.add(new Paragraph("用法：" + rx.getNotes()).setFont(font).setFontSize(9));
            doc.add(new Paragraph("医师签名：" + doctor.getName()).setFont(font).setFontSize(9));
            doc.close(); return baos.toByteArray();
        } catch (Exception e) { log.error("PDF error", e); throw new BusinessException(ErrorCode.PDF_GENERATION_FAILED); }
    }

    public byte[] generatePrescriptionPreview(Prescription rx, List<PrescriptionItem> items, User doctor, String patientName) {
        return generatePrescriptionPdf(rx, items, doctor, patientName, null);
    }
}
