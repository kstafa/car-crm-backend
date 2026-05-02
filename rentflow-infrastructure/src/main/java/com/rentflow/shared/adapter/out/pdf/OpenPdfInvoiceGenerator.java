package com.rentflow.shared.adapter.out.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rentflow.contract.model.ContractDetail;
import com.rentflow.payment.LineItem;
import com.rentflow.payment.Payment;
import com.rentflow.payment.model.InvoiceDetail;
import com.rentflow.shared.InfrastructureException;
import com.rentflow.shared.money.Money;
import com.rentflow.shared.port.out.PdfGeneratorPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.awt.Color;
import java.math.RoundingMode;

@Component
@Primary
public class OpenPdfInvoiceGenerator implements PdfGeneratorPort {

    @Override
    public byte[] generateInvoice(InvoiceDetail invoice) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();
            addHeader(doc, invoice);
            addBillTo(doc, invoice);
            addLineItemsTable(doc, invoice);
            addTotals(doc, invoice);
            if (!invoice.payments().isEmpty()) {
                addPaymentHistory(doc, invoice);
            }
            addFooter(doc);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new InfrastructureException("Failed to generate invoice PDF", e);
        }
    }

    @Override
    public byte[] generateContract(ContractDetail contract) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document();
            PdfWriter.getInstance(doc, baos);
            doc.open();
            doc.add(new Paragraph("Contract: " + contract.contractNumber()));
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new InfrastructureException("Failed to generate contract PDF", e);
        }
    }

    private void addHeader(Document doc, InvoiceDetail invoice) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        Font grayFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60f, 40f});
        header.setSpacingAfter(20);

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("RentFlow Car Rental", titleFont));
        left.addElement(new Paragraph("123 Avenue de la Gare\n75001 Paris, France", grayFont));
        header.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("INVOICE", titleFont));
        right.addElement(new Paragraph("# " + invoice.invoiceNumber(), normalFont));
        right.addElement(new Paragraph("Issue date: " + invoice.issueDate(), grayFont));
        right.addElement(new Paragraph("Due date: " + invoice.dueDate(), grayFont));
        right.addElement(new Paragraph("Status: " + invoice.status().name(), grayFont));
        header.addCell(right);

        doc.add(header);
    }

    private void addBillTo(Document doc, InvoiceDetail invoice) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        doc.add(new Paragraph("Bill To", sectionFont));
        doc.add(new Paragraph("Customer ID: " + invoice.customerId().value(), normalFont));
        doc.add(Chunk.NEWLINE);
    }

    private void addLineItemsTable(Document doc, InvoiceDetail invoice) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{45f, 15f, 20f, 20f});
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);

        Color headerBg = new Color(30, 30, 30);
        for (String col : new String[]{"Description", "Qty", "Unit Price", "Total"}) {
            PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setPadding(8);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean alternate = false;
        for (LineItem item : invoice.lineItems()) {
            Color rowBg = alternate ? new Color(245, 245, 245) : Color.WHITE;
            alternate = !alternate;
            for (String value : new String[]{item.description(), String.valueOf(item.quantity()),
                    formatMoney(item.unitPrice()), formatMoney(item.total())}) {
                PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(7);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }
        }
        doc.add(table);
    }

    private void addTotals(Document doc, InvoiceDetail invoice) throws DocumentException {
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(40);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{60f, 40f});
        totals.setSpacingAfter(20);

        addTotalRow(totals, "Total", formatMoney(invoice.totalAmount()), boldFont, normalFont);
        addTotalRow(totals, "Paid", formatMoney(invoice.paidAmount()), normalFont, normalFont);
        addTotalRow(totals, "Outstanding", formatMoney(invoice.outstandingAmount()), boldFont, normalFont);
        doc.add(totals);
    }

    private void addPaymentHistory(Document doc, InvoiceDetail invoice) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        doc.add(new Paragraph("Payment History", sectionFont));
        for (Payment payment : invoice.payments()) {
            doc.add(new Paragraph(payment.paidAt() + "  " + payment.method().name() + "  "
                    + formatMoney(payment.amount())
                    + (payment.gatewayReference() == null ? "" : "  Ref: " + payment.gatewayReference()),
                    normalFont));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addFooter(Document doc) throws DocumentException {
        Font grayFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
        doc.add(new Paragraph("Thank you for choosing RentFlow. For questions, contact billing@rentflow.com",
                grayFont));
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String formatMoney(Money money) {
        return money.currency().getSymbol() + " "
                + money.amount().setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
