# Equipos Webapp (Railway + GitHub)

Webapp mínima con Express para desplegar en Railway mediante GitHub.

## Requisitos
- Node.js 18+ (LTS)
- Cuenta de GitHub
- Cuenta en Railway

## Ejecutar en local
```bash
npm install
npm run dev
# abrir http://localhost:3000
```

## Estructura
- `server.js`: servidor Express que sirve `/public` y `/health`.
- `public/index.html`, `public/app.js`, `public/styles.css`
- `package.json`: `start` y `dev` scripts, `engines.node >=18`.

## Despliegue en Railway usando GitHub (monorepo)
1. Crea un repositorio en GitHub y sube el proyecto (esta carpeta incluida).
2. En Railway: New Project → Deploy from GitHub Repo → selecciona tu repo.
3. Cuando Railway pregunte por el directorio del servicio, elige la subcarpeta:
   - Root/Service directory: `webapp`
4. Comando de inicio: `npm start` (ya definido en package.json).
5. Railway define `PORT` automáticamente (el servidor ya lo usa).
6. Espera el build/deploy. Abre la URL pública que te da Railway.

## Git: comandos rápidos
Desde la raíz del repo (no dentro de `webapp`), por ejemplo `c:/Users/Ariel/Documents/Android cosas/Equipos/`:
```powershell
# inicializar (si aún no es repo)
git init
# si aún no existe .gitignore para Node en webapp, ya hay uno en webapp/.gitignore
# agregar todo
git add .
# commit inicial
git commit -m "chore(webapp): scaffold express for railway"
# crear repo en GitHub (GUI) y copiar URL (HTTPS)
# ejemplo:
# git remote add origin https://github.com/<usuario>/<repo>.git
# git branch -M main
# git push -u origin main
```

## Notas
- Si necesitas CI/CD, se puede agregar un workflow de GitHub Actions para validar que el server arranca.
- Para features dinámicas (API, BD, etc.), se pueden añadir rutas Express y variables de entorno en Railway.
