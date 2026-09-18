package com.elitelearn;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle; // FIX: Added correct import for A4 size
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestGenerator {

    private static final float MARGIN_LEFT = 50;
    private static final float MARGIN_RIGHT = 50;
    private static final float LINE_HEIGHT = 20;
    private static final float WRITING_LINE_HEIGHT = 24;

    // ==========================================
    // TEST PDF
    // ==========================================
    public static void generateTestPDF(String title, String teacherName, List<Flashcard> questions,
                                       File outputFile) throws IOException {
        PDDocument document = new PDDocument();

        try {
            // FIX: Use PDRectangle.A4 instead of PDPage.PAGE_SIZE_A4
            float pageWidth = PDRectangle.A4.getWidth();

            int questionIndex = 0;
            int pageNum = 0;
            int totalPages = estimateTotalPages(questions);

            while (questionIndex < questions.size()) {
                // FIX: Use PDRectangle.A4 for page creation
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                pageNum++;
                boolean isFirstPage = (pageNum == 1);

                PDPageContentStream cs = new PDPageContentStream(document, page);

                // --- Header ---
                float yPos;

                if (isFirstPage) {
                    // First Page: Bigger title, more spacing
                    float titleSize = 24;
                    float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * titleSize;
                    float titleX = (pageWidth - titleWidth) / 2;

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, titleSize);
                    cs.newLineAtOffset(titleX, 750);
                    cs.showText(title);
                    cs.endText();

                    // Teacher Name (Right-aligned, no "Teacher: " prefix)
                    if (!teacherName.trim().isEmpty()) {
                        float nameSize = 12;
                        float nameWidth = PDType1Font.HELVETICA.getStringWidth(teacherName) / 1000 * nameSize;
                        float nameX = pageWidth - MARGIN_RIGHT - nameWidth;
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, nameSize);
                        cs.newLineAtOffset(nameX, 720);
                        cs.showText(teacherName);
                        cs.endText();
                    }

                    // Student Name Field (First page only)
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(MARGIN_LEFT, 695);
                    cs.showText("Name: _______________________________________________");
                    cs.endText();

                    yPos = 655; // More space before first question

                } else {
                    // Subsequent Pages: Slightly smaller title, tighter spacing
                    float titleSize = 18;
                    float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(title) / 1000 * titleSize;
                    float titleX = (pageWidth - titleWidth) / 2;

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, titleSize);
                    cs.newLineAtOffset(titleX, 760);
                    cs.showText(title);
                    cs.endText();

                    // Teacher Name (Right-aligned)
                    if (!teacherName.trim().isEmpty()) {
                        float nameSize = 12;
                        float nameWidth = PDType1Font.HELVETICA.getStringWidth(teacherName) / 1000 * nameSize;
                        float nameX = pageWidth - MARGIN_RIGHT - nameWidth;
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, nameSize);
                        cs.newLineAtOffset(nameX, 738);
                        cs.showText(teacherName);
                        cs.endText();
                    }

                    yPos = 710; // Consistent tighter spacing
                }

                // --- Questions ---
                while (questionIndex < questions.size()) {
                    Flashcard card = questions.get(questionIndex);
                    int qNum = questionIndex + 1;
                    String qNumStr = qNum + ". ";

                    if (yPos < 100) break;

                    // Draw question number (bold)
                    float qNumWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(qNumStr) / 1000 * 12;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(MARGIN_LEFT, yPos);
                    cs.showText(qNumStr);
                    cs.endText();

                    // Draw question text (word-wrapped)
                    float questionStartX = MARGIN_LEFT + qNumWidth;
                    float questionMaxWidth = pageWidth - questionStartX - MARGIN_RIGHT;
                    yPos = drawWrappedText(cs, PDType1Font.HELVETICA, 12,
                            card.getQuestion(), questionStartX, yPos, questionMaxWidth, LINE_HEIGHT);

                    yPos -= 6; // Small gap before writing lines

                    // Draw blank lines for student to write their answer
                    int lines = Math.max(1, card.getLinesNeeded());
                    for (int l = 0; l < lines; l++) {
                        if (yPos < 60) break; 
                        cs.setLineWidth(0.3f);
                        cs.moveTo(MARGIN_LEFT, yPos);
                        cs.lineTo(pageWidth - MARGIN_RIGHT, yPos);
                        cs.stroke();
                        yPos -= WRITING_LINE_HEIGHT;
                    }

                    yPos -= 12; // Gap between questions
                    questionIndex++;
                }

                // --- Page Number (Bottom Center) ---
                String pageStr = "Page " + pageNum + " of " + totalPages;
                float pageStrWidth = PDType1Font.HELVETICA.getStringWidth(pageStr) / 1000 * 10;
                float pageStrX = (pageWidth - pageStrWidth) / 2;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(pageStrX, 30);
                cs.showText(pageStr);
                cs.endText();

                cs.close();
            }

            document.save(outputFile);
        } finally {
            document.close();
        }
    }

    // ==========================================
    // ANSWER KEY PDF
    // ==========================================
    public static void generateAnswerKeyPDF(String title, String teacherName, List<Flashcard> questions,
                                            File outputFile) throws IOException {
        PDDocument document = new PDDocument();

        try {
            // FIX: Use PDRectangle.A4
            float pageWidth = PDRectangle.A4.getWidth();
            String fullTitle = title + " - ANSWER KEY";

            int questionIndex = 0;
            int pageNum = 0;
            int totalPages = estimateAnswerKeyPages(questions);

            while (questionIndex < questions.size()) {
                // FIX: Use PDRectangle.A4
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                pageNum++;

                PDPageContentStream cs = new PDPageContentStream(document, page);

                // --- Header ---
                float titleSize = 18;
                float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(fullTitle) / 1000 * titleSize;
                float titleX = (pageWidth - titleWidth) / 2;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, titleSize);
                cs.newLineAtOffset(titleX, 760);
                cs.showText(fullTitle);
                cs.endText();

                // Teacher Name (Right-aligned)
                if (!teacherName.trim().isEmpty()) {
                    float nameSize = 12;
                    float nameWidth = PDType1Font.HELVETICA.getStringWidth(teacherName) / 1000 * nameSize;
                    float nameX = pageWidth - MARGIN_RIGHT - nameWidth;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, nameSize);
                    cs.newLineAtOffset(nameX, 738);
                    cs.showText(teacherName);
                    cs.endText();
                }

                float yPos = 710;

                // --- Answers ---
                while (questionIndex < questions.size()) {
                    Flashcard card = questions.get(questionIndex);
                    int qNum = questionIndex + 1;
                    String qNumStr = qNum + ". ";

                    if (yPos < 80) break;

                    // Question Number (bold)
                    float qNumWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(qNumStr) / 1000 * 12;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.newLineAtOffset(MARGIN_LEFT, yPos);
                    cs.showText(qNumStr);
                    cs.endText();

                    // Answer Text (word-wrapped to prevent clipping!)
                    float answerStartX = MARGIN_LEFT + qNumWidth;
                    float answerMaxWidth = pageWidth - answerStartX - MARGIN_RIGHT;
                    yPos = drawWrappedText(cs, PDType1Font.HELVETICA, 12,
                            card.getAnswer(), answerStartX, yPos, answerMaxWidth, LINE_HEIGHT);

                    yPos -= 10; // Gap between answers
                    questionIndex++;
                }

                // --- Page Number (Bottom Center) ---
                String pageStr = "Page " + pageNum + " of " + totalPages;
                float pageStrWidth = PDType1Font.HELVETICA.getStringWidth(pageStr) / 1000 * 10;
                float pageStrX = (pageWidth - pageStrWidth) / 2;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.newLineAtOffset(pageStrX, 30);
                cs.showText(pageStr);
                cs.endText();

                cs.close();
            }

            document.save(outputFile);
        } finally {
            document.close();
        }
    }

    // ==========================================
    // HELPER: Word-Wrapped Text Drawing
    // ==========================================
    private static float drawWrappedText(PDPageContentStream cs, PDType1Font font, float fontSize,
                                          String text, float x, float y, float maxWidth,
                                          float lineHeight) throws IOException {
        if (text == null || text.isEmpty()) return y - lineHeight;

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float currentY = y;

        for (String word : words) {
            String testLine = line.length() == 0 ? word : line + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (testWidth > maxWidth && line.length() > 0) {
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.newLineAtOffset(x, currentY);
                cs.showText(line.toString());
                cs.endText();
                currentY -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }

        if (line.length() > 0) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, currentY);
            cs.showText(line.toString());
            cs.endText();
            currentY -= lineHeight;
        }

        return currentY;
    }

    // ==========================================
    // HELPERS: Page Estimation
    // ==========================================
    private static int estimateTotalPages(List<Flashcard> questions) {
        if (questions.isEmpty()) return 1;
        int count = questions.size();
        if (count <= 3) return 1;
        return 1 + (int) Math.ceil((double) (count - 3) / 4);
    }

    private static int estimateAnswerKeyPages(List<Flashcard> questions) {
        int answersPerPage = 8;
        return Math.max(1, (int) Math.ceil((double) questions.size() / answersPerPage));
    }
}