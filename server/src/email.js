const { Resend } = require('resend');

const apiKey = (process.env.RESEND_API_KEY || '').trim();
const mailFrom = (process.env.MAIL_FROM || '').trim();
const resend = apiKey ? new Resend(apiKey) : null;

if (!apiKey) console.warn('[email][resend] RESEND_API_KEY not set');

async function internalSend(msg) {
  if (!apiKey) throw new Error('email_provider_not_configured');
  if (!mailFrom) throw new Error('mail_from_not_configured');
  const { data, error } = await resend.emails.send(msg);
  if (error || !data?.id) throw new Error(error?.message || 'resend_failed');
}

async function sendVerificationEmail(to, code) {
  const subject = 'Verificación de cuenta';
  const html = `<h1>Verificación</h1><p>Tu código es: <strong>${code}</strong></p>`;
  await internalSend({ to, from: mailFrom, subject, html });
  console.log('[email][resend] Email enviado a:', to);
}

async function sendVerificationLink(user, link) {
  const subject = 'Verifica tu correo';
  const text = `Hola${user.name ? ' ' + user.name : ''}, verifica tu correo: ${link}`;
  const html = `<p>Hola${user.name ? ' ' + user.name : ''},</p><p>Verifica tu correo haciendo clic en el siguiente enlace:</p><p><a href="${link}">Verificar correo</a></p><p>Este enlace caduca en 24 horas.</p>`;
  await internalSend({ to: user.email, from: mailFrom, subject, text, html });
  console.log('[email][resend] Email de verificación enviado a:', user.email);
}

module.exports = { sendVerificationEmail, sendVerificationLink, isEmailConfigured: () => Boolean(apiKey && mailFrom) };
