/**
 * Configuración validada al arrancar.
 *
 * El secreto de firma no tiene valor por defecto a propósito. Antes el código
 * hacía `process.env.JWT_SECRET || 'dev_secret'` en tres archivos distintos: si
 * la variable faltaba en producción el servidor arrancaba igual y firmaba todos
 * los tokens con una constante que está publicada en el repositorio, de modo que
 * cualquiera podía emitir un token válido para cualquier usuario. Un servidor
 * que no arranca es un incidente visible; uno que arranca inseguro, no.
 */

const MIN_SECRET_LENGTH = 32;

/** Valores que alguna vez estuvieron en el repositorio: quedan quemados para siempre. */
const LEAKED_SECRETS = new Set(['dev_secret', '575843']);

const HOW_TO_GENERATE =
  '  Generá uno con:\n' +
  '    node -e "console.log(require(\'crypto\').randomBytes(32).toString(\'hex\'))"\n' +
  '  y cargalo como variable de entorno JWT_SECRET.\n' +
  '  En Render: Dashboard -> el servicio -> Environment -> Add Environment Variable.';

function fail(reason) {
  throw new Error('\n\n[config] ' + reason + '\n' + HOW_TO_GENERATE + '\n');
}

function readJwtSecret() {
  const secret = (process.env.JWT_SECRET || '').trim();

  if (!secret) {
    fail('JWT_SECRET no está definido. El servidor no arranca sin él.');
  }
  if (LEAKED_SECRETS.has(secret.toLowerCase())) {
    fail(
      'JWT_SECRET tiene un valor que estuvo versionado en el repositorio y debe ' +
      'considerarse público. Hay que reemplazarlo por uno nuevo.'
    );
  }
  if (secret.length < MIN_SECRET_LENGTH) {
    fail(
      'JWT_SECRET es demasiado corto: ' + secret.length + ' caracteres, ' +
      'el mínimo es ' + MIN_SECRET_LENGTH + '. Un secreto corto se rompe por fuerza ' +
      'bruta contra cualquier token capturado, sin tocar el servidor.'
    );
  }
  // Un número largo tiene mucha menos entropía por carácter que una cadena
  // aleatoria: 32 dígitos son ~106 bits, pero 32 caracteres hexadecimales son 128.
  if (new Set(secret).size < 8) {
    fail(
      'JWT_SECRET tiene muy poca variedad de caracteres (' + new Set(secret).size +
      ' distintos). Parece una cadena repetitiva o un número, no un valor aleatorio.'
    );
  }

  return secret;
}

module.exports = {
  JWT_SECRET: readJwtSecret(),
  MIN_SECRET_LENGTH,
};
