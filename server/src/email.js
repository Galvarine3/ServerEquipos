const { Resend } = require('resend');

const resend = new Resend(process.env.RESEND_API_KEY);

async function sendVerificationEmail(to, code) {
  try {
    await resend.emails.send({
      from: 'noreply@resend.dev',
      to,
      subject: 'Verificación de cuenta',
      html: `
        <h1>Verificación</h1>
        <p>Tu código es: <strong>${code}</strong></p>
      `,
    });

    console.log('Email enviado a:', to);
  } catch (err) {
    console.error('Error enviando email:', err);
  }
}

module.exports = { sendVerificationEmail };
