const apiKey = (process.env.RESEND_API_KEY || '').trim();
const mailFrom = (process.env.MAIL_FROM || '').trim();

if (!apiKey) console.warn('[email][resend] RESEND_API_KEY not set');

async function internalSend(msg) {
  if (!apiKey) throw new Error('email_provider_not_configured');
  if (!mailFrom) throw new Error('mail_from_not_configured');
  let response;
  try {
    response = await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(msg)
    });
  } catch (error) {
    throw new Error(`resend_network_failed: ${error.message}`);
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok || !data.id) {
    throw new Error(data.message || data.name || `resend_failed_${response.status}`);
  }
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
