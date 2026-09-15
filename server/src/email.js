const apiKey = (process.env.BREVO_API_KEY || '').trim();
const mailFrom = (process.env.MAIL_FROM || '').trim();

if (!apiKey) console.warn('[email][brevo] BREVO_API_KEY not set');

async function internalSend(msg) {
  if (!apiKey) throw new Error('email_provider_not_configured');
  if (!mailFrom) throw new Error('mail_from_not_configured');
  let response;
  try {
    response = await fetch('https://api.brevo.com/v3/smtp/email', {
      method: 'POST',
      headers: {
        'api-key': apiKey,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sender: { email: msg.from },
        to: [{ email: msg.to }],
        subject: msg.subject,
        htmlContent: msg.html,
        textContent: msg.text
      })
    });
  } catch (error) {
    throw new Error(`brevo_network_failed: ${error.message}`);
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok || !data.messageId) {
    throw new Error(data.message || data.code || `brevo_failed_${response.status}`);
  }
}

async function sendVerificationEmail(to, code) {
  const subject = 'Verificación de cuenta';
  const html = `<h1>Verificación</h1><p>Tu código es: <strong>${code}</strong></p>`;
  await internalSend({ to, from: mailFrom, subject, html });
  console.log('[email][brevo] Email enviado a:', to);
}

async function sendVerificationLink(user, link) {
  const subject = 'Verifica tu correo';
  const text = `Hola${user.name ? ' ' + user.name : ''}, verifica tu correo: ${link}`;
  const html = `<p>Hola${user.name ? ' ' + user.name : ''},</p><p>Verifica tu correo haciendo clic en el siguiente enlace:</p><p><a href="${link}">Verificar correo</a></p><p>Este enlace caduca en 24 horas.</p>`;
  await internalSend({ to: user.email, from: mailFrom, subject, text, html });
  console.log('[email][brevo] Email de verificación enviado a:', user.email);
}

async function sendPasswordResetCode(to, code, minutes) {
  const subject = 'Codigo para restablecer tu contrasena';
  const text = `Tu codigo para restablecer la contrasena es ${code}. Caduca en ${minutes} minutos. `
    + `Si no lo pediste, ignora este correo: tu contrasena no cambia sola.`;
  const html = `<p>Tu c\u00f3digo para restablecer la contrase\u00f1a es:</p>`
    + `<p style="font-size:28px;font-weight:bold;letter-spacing:6px;margin:16px 0">${code}</p>`
    + `<p>Caduca en ${minutes} minutos y sirve una sola vez.</p>`
    + `<p style="color:#666;font-size:13px">Si no lo pediste, ignora este correo: tu contrase\u00f1a no cambia sola.</p>`;
  await internalSend({ to, from: mailFrom, subject, text, html });
  console.log('[email][brevo] Codigo de recuperacion enviado a:', to);
}

module.exports = { sendVerificationEmail, sendVerificationLink, sendPasswordResetCode, isEmailConfigured: () => Boolean(apiKey && mailFrom) };
