package com.bradox.erp.integration;

import com.bradox.erp.sign.domain.core.model.SignLimits;
import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import com.bradox.erp.sign.service.domain.dto.PageImage;
import com.bradox.erp.sign.service.domain.ports.output.PdfPort;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.AttributedString;
import java.text.Bidi;
import java.util.List;

/**
 * PDF and image work for Sign: page images for the viewer, flattening the signed values onto the original pages, and
 * cleaning drawn signature images. Built on PDFBox; the domain never sees it.
 *
 * <p>PDFBox cannot shape Arabic or Kurdish text, so any text that is not plain Latin is drawn through Java2D, which does
 * shape and reorder right-to-left scripts, and placed as an image. That needs a font with the glyphs on the server
 * (DejaVu Sans is enough); the final PDF then shows the right letters but that text is not selectable.
 */
@Component
public class SignPdfAdapter implements PdfPort {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final int MAX_SIGNATURE_PIXELS = 1600;
    private static final int TEXT_IMAGE_SCALE = 4;

    // ------------------------------------------------------------------ inspect and render

    @Override
    public PdfInfo inspect(byte[] pdf) {
        if (pdf == null || pdf.length == 0 || pdf.length > SignLimits.MAX_PDF_BYTES) {
            throw new SignDomainException("error.sign.pdf.size", null, "The PDF must be at most 25 MB");
        }
        if (pdf.length < 5 || pdf[0] != '%' || pdf[1] != 'P' || pdf[2] != 'D' || pdf[3] != 'F') {
            throw new SignDomainException("error.sign.pdf.notPdf", null, "This file is not a PDF");
        }
        try (PDDocument doc = PDDocument.load(pdf)) {
            if (doc.isEncrypted()) {
                throw new SignDomainException("error.sign.pdf.encrypted", null, "Password-protected PDFs cannot be signed");
            }
            int pages = doc.getNumberOfPages();
            if (pages < 1 || pages > SignLimits.MAX_PDF_PAGES) {
                throw new SignDomainException("error.sign.pdf.pages", new Object[]{SignLimits.MAX_PDF_PAGES},
                        "The PDF must have between 1 and " + SignLimits.MAX_PDF_PAGES + " pages");
            }
            if (!doc.getSignatureDictionaries().isEmpty()) {
                throw new SignDomainException("error.sign.pdf.alreadySigned", null,
                        "This PDF already has digital signatures that adding fields would invalidate");
            }
            return new PdfInfo(pages);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("password")) {
                throw new SignDomainException("error.sign.pdf.encrypted", null, "Password-protected PDFs cannot be signed");
            }
            throw new SignDomainException("error.sign.pdf.unreadable", null, "This PDF could not be read");
        }
    }

    @Override
    public PageImage render(byte[] pdf, int page, float scale) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            BufferedImage image = new PDFRenderer(doc).renderImageWithDPI(page - 1, 72f * scale, ImageType.RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new PageImage(out.toByteArray(), image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new SignDomainException("error.sign.pdf.unreadable", null, "This PDF could not be read");
        }
    }

    // ------------------------------------------------------------------ signature images

    @Override
    public byte[] sanitizeSignatureImage(byte[] png) {
        if (png == null || png.length < 8 || (png[0] & 0xFF) != 0x89 || png[1] != 'P' || png[2] != 'N' || png[3] != 'G') {
            throw new SignDomainException("error.sign.image.invalid", null, "The signature image must be a PNG");
        }
        try {
            BufferedImage in = ImageIO.read(new ByteArrayInputStream(png));
            if (in == null || in.getWidth() < 1 || in.getHeight() < 1 || (long) in.getWidth() * in.getHeight() > 25_000_000L) {
                throw new SignDomainException("error.sign.image.invalid", null, "The signature image is not valid");
            }
            double factor = Math.min(1.0, (double) MAX_SIGNATURE_PIXELS / Math.max(in.getWidth(), in.getHeight()));
            for (int attempt = 0; attempt < 6; attempt++) {
                int w = Math.max(1, (int) Math.round(in.getWidth() * factor));
                int h = Math.max(1, (int) Math.round(in.getHeight() * factor));
                BufferedImage clean = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);   // a fresh canvas drops all metadata
                Graphics2D g = clean.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(in, 0, 0, w, h, null);
                g.dispose();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(clean, "png", out);
                if (out.size() <= SignLimits.MAX_SIGNATURE_IMAGE_BYTES) {
                    return out.toByteArray();
                }
                factor *= 0.7;
            }
            throw new SignDomainException("error.sign.image.tooBig", null, "The signature image is too large");
        } catch (IOException | RuntimeException e) {
            if (e instanceof SignDomainException sde) {
                throw sde;
            }
            throw new SignDomainException("error.sign.image.invalid", null, "The signature image is not valid");
        }
    }

    // ------------------------------------------------------------------ final PDF

    @Override
    public byte[] applyValues(byte[] sourcePdf, List<Placement> placements) {
        try (PDDocument doc = PDDocument.load(sourcePdf)) {
            for (Placement p : placements) {
                if (p.page() < 1 || p.page() > doc.getNumberOfPages()) {
                    continue;
                }
                draw(doc, doc.getPage(p.page() - 1), p);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new SignDomainException("error.sign.pdf.build", null, "The signed PDF could not be built");
        }
    }

    private void draw(PDDocument doc, PDPage page, Placement p) throws IOException {
        PDRectangle crop = page.getCropBox();
        int rotation = ((page.getRotation() % 360) + 360) % 360;
        float uw = crop.getWidth();
        float uh = crop.getHeight();
        boolean sideways = rotation == 90 || rotation == 270;
        float dw = sideways ? uh : uw;          // size of the page as the reader sees it
        float dh = sideways ? uw : uh;
        float bx = (float) (p.x() * dw);
        float bw = (float) (p.width() * dw);
        float bh = (float) (p.height() * dh);
        float by = dh - (float) (p.y() * dh) - bh;          // lower-left corner, y up, in displayed space

        try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            cs.transform(Matrix.getTranslateInstance(crop.getLowerLeftX(), crop.getLowerLeftY()));
            switch (rotation) {
                case 90 -> cs.transform(new Matrix(0, 1, -1, 0, uw, 0));
                case 180 -> cs.transform(new Matrix(-1, 0, 0, -1, uw, uh));
                case 270 -> cs.transform(new Matrix(0, -1, 1, 0, 0, uh));
                default -> { }
            }
            FieldType type = p.type();
            if (type == FieldType.SIGNATURE && p.image() != null) {
                PDImageXObject img = PDImageXObject.createFromByteArray(doc, p.image(), "signature");
                fitImage(cs, img, bx, by, bw, bh);
            } else if (p.text() != null && !p.text().isBlank()) {
                float size = Math.max(6f, Math.min(bh * 0.72f, 14f));
                drawText(doc, cs, p.text().trim(), bx + 2, by + (bh - size) / 2 + size * 0.2f, size, bw - 4);
            }
        }
    }

    private void fitImage(PDPageContentStream cs, PDImageXObject img, float x, float y, float w, float h) throws IOException {
        float ratio = (float) img.getWidth() / img.getHeight();
        float tw = w;
        float th = tw / ratio;
        if (th > h) {
            th = h;
            tw = th * ratio;
        }
        cs.drawImage(img, x + (w - tw) / 2, y + (h - th) / 2, tw, th);
    }

    // ------------------------------------------------------------------ text

    private static boolean isLatin(String s) {
        return s.chars().allMatch(c -> c >= 0x20 && c <= 0x7E || c >= 0xA0 && c <= 0xFF);
    }

    /** Plain Latin goes in as real text; anything else is shaped by Java2D and placed as an image. */
    private void drawText(PDDocument doc, PDPageContentStream cs, String text, float x, float y, float size, float maxWidth)
            throws IOException {
        if (isLatin(text)) {
            PDFont font = PDType1Font.HELVETICA;
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(0, 0, 0);
            cs.newLineAtOffset(x, y);
            cs.showText(text);
            cs.endText();
            return;
        }
        BufferedImage img = renderText(text, size * TEXT_IMAGE_SCALE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        PDImageXObject pd = PDImageXObject.createFromByteArray(doc, out.toByteArray(), "text");
        float w = (float) img.getWidth() / TEXT_IMAGE_SCALE;
        float h = (float) img.getHeight() / TEXT_IMAGE_SCALE;
        if (w > maxWidth && maxWidth > 0) {
            float k = maxWidth / w;
            w *= k;
            h *= k;
        }
        cs.drawImage(pd, x, y - h * 0.25f, w, h);
    }

    private BufferedImage renderText(String text, float pixelSize) {
        Font font = new Font("SansSerif", Font.PLAIN, Math.round(pixelSize));
        FontRenderContext frc = new FontRenderContext(null, true, true);
        boolean rtl = Bidi.requiresBidi(text.toCharArray(), 0, text.length())
                && new Bidi(text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).baseIsLeftToRight() == false;
        AttributedString as = new AttributedString(text);
        as.addAttribute(java.awt.font.TextAttribute.FONT, font);
        as.addAttribute(java.awt.font.TextAttribute.RUN_DIRECTION,
                rtl ? java.awt.font.TextAttribute.RUN_DIRECTION_RTL : java.awt.font.TextAttribute.RUN_DIRECTION_LTR);
        TextLayout layout = new TextLayout(as.getIterator(), frc);
        Rectangle2D b = layout.getBounds();
        int w = (int) Math.ceil(layout.getAdvance()) + 4;
        int h = (int) Math.ceil(layout.getAscent() + layout.getDescent()) + 4;
        BufferedImage img = new BufferedImage(Math.max(w, 1), Math.max(h, 1), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setColor(Color.BLACK);
        layout.draw(g, 2, 2 + layout.getAscent());
        g.dispose();
        return img;
    }
}
