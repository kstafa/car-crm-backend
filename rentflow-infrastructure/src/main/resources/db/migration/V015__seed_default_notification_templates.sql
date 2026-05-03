INSERT INTO notification_templates (id, name, trigger, channel, subject_template, body_template)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01',
    'Reservation Confirmed Email',
    'RESERVATION_CONFIRMED',
    'EMAIL',
    'Your booking {{reservationNumber}} is confirmed',
    '<p>Dear {{customerName}},</p>
     <p>Your reservation <strong>{{reservationNumber}}</strong> has been confirmed.</p>
     <p>Pickup: {{pickupDate}}<br>Return: {{returnDate}}</p>
     <p>Thank you for choosing RentFlow.</p>'
);

INSERT INTO notification_templates (id, name, trigger, channel, subject_template, body_template)
VALUES (
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee02',
    'Invoice Sent Email',
    'INVOICE_SENT',
    'EMAIL',
    'Invoice {{invoiceId}} from RentFlow',
    '<p>Dear {{customerName}},</p>
     <p>Your invoice is ready. Please log in to view and pay.</p>
     <p>Thank you,<br>RentFlow Billing</p>'
);

INSERT INTO automation_rules (id, name, trigger, template_id, active, delay_minutes)
VALUES (
    'ffffffff-ffff-ffff-ffff-ffffffffffff',
    'Send confirmation email on reservation confirmed',
    'RESERVATION_CONFIRMED',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeee01',
    TRUE,
    0
);
