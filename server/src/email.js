const sgMail = require('@sendgrid/mail');

const apiKey = process.env.SENDGRID_API_KEY;

if (apiKey) {
  sgMail.setApiKey(apiKey);
} else {
  console.warn('[email][sendgrid] SENDGRID_API_KEY not set');
}

async function internalSend(msg) {
  if (!apiKey) {
    console.log('[email][sendgrid] not configured, would send:', { to: msg.to, subject: msg.subject });
    return;
  }
  const [response] = await sgMail.send(msg);
  if (!response || response.statusCode >= 400) {
    throw new Error('sendgrid_failed');
  }
}

async function sendVerificationEmail(to, code) {
  const from = process.env.MAIL_FROM || 'no-reply@example.com';
  const subject = 'Verificación de cuenta';
  const html = `<h1>Verificación</h1><p>Tu código es: <strong>${code}</strong></p>`;
  try {
    await internalSend({ to, from, subject, html });
    console.log('Email enviado a:', to);
  } catch (err) {
    console.error('[email][sendgrid] Error enviando email:', err);
  }
}

async function sendVerificationLink(user, link) {
  const from = process.env.MAIL_FROM || 'no-reply@example.com';
  const subject = 'Verifica tu correo';
  const text = `Hola${user.name ? ' ' + user.name : ''}, verifica tu correo: ${link}`;
  const html = `<p>Hola${user.name ? ' ' + user.name : ''},</p><p>Verifica tu correo haciendo clic en el siguiente enlace:</p><p><a href="${link}">Verificar correo</a></p>`;
  try {
    await internalSend({ to: user.email, from, subject, text, html });
  } catch (err) {
    console.error('[email][sendgrid] Error enviando email de verificación:', err);
    console.log('[auth] verification link:', link);
  }
}

module.exports = { sendVerificationEmail, sendVerificationLink };
