# 📱 Equipos — Backend

Backend de **Equipos**, una aplicación móvil orientada a la gestión, organización y comunidad deportiva.

El servidor proporciona la infraestructura necesaria para administrar **usuarios, jugadores, partidos, torneos, ligas, formaciones y comunidad**, manteniendo la información de cada usuario almacenada y sincronizada online.

Cada usuario dispone de una cuenta personal a la que se asocia su información deportiva. Esto permite que sus datos permanezcan disponibles al iniciar sesión desde diferentes dispositivos.

---

# 🏗️ Arquitectura

La aplicación móvil funciona como cliente y se comunica con el backend mediante **API REST y WebSockets**.

```text
┌──────────────────────────┐
│      App Android         │
│        Equipos           │
└────────────┬─────────────┘
             │
             │ Internet
             │ REST / WebSocket
             ▼
┌──────────────────────────┐
│         Backend          │
│    Node.js + Express     │
└────────────┬─────────────┘
             │
             ▼
┌──────────────────────────┐
│       PostgreSQL         │
│    Información online    │
└──────────────────────────┘
```

El backend procesa las solicitudes provenientes de la aplicación, gestiona la autenticación, administra los datos y mantiene la información persistente en la base de datos.

---

# 👤 Gestión de usuarios

Cada usuario dispone de una **cuenta personal**, desde la cual puede administrar su información y actividades deportivas.

Los datos se encuentran asociados a la cuenta correspondiente, permitiendo mantenerlos almacenados online.

Entre la información asociada a cada usuario se encuentran:

* Perfil del usuario.
* Jugadores.
* Equipos.
* Partidos.
* Torneos.
* Ligas.
* Formaciones.
* Publicaciones.
* Actividades de la comunidad.

Esto permite que el usuario pueda recuperar su información al iniciar sesión desde otro dispositivo, sin depender exclusivamente del almacenamiento local del teléfono.

---

# 🏃 Gestión de jugadores

La aplicación permite crear y administrar **jugadores personalizados**.

Los jugadores pueden incorporar información utilizada para determinar su nivel dentro de las actividades deportivas.

Una de las funcionalidades principales es el sistema de **jugadores rankeados**, que permite asignar una valoración o nivel a los participantes.

Este sistema puede utilizarse posteriormente para organizar partidos y distribuir jugadores de manera más equilibrada.

La gestión de jugadores permite mantener una base de participantes asociada a la cuenta del usuario y disponible online.

---

# ⚖️ Partidos balanceados

Equipos permite organizar partidos utilizando un sistema de distribución de jugadores orientado a generar **equipos equilibrados**.

El sistema puede considerar el ranking o nivel de los jugadores para distribuirlos entre los diferentes equipos.

Esto permite reducir las diferencias de nivel y facilitar la creación de encuentros competitivos.

El usuario puede utilizar esta funcionalidad para organizar partidos de forma rápida sin tener que realizar manualmente la distribución de los participantes.

---

# 🎯 Partidos personalizados

Además de los partidos balanceados, el usuario puede organizar encuentros de acuerdo con sus propias preferencias.

Es posible seleccionar manualmente los jugadores y definir la composición de los equipos.

De esta forma, la aplicación permite trabajar tanto con:

* Partidos balanceados automáticamente.
* Selección manual de jugadores.
* Equipos definidos por el usuario.
* Diferentes configuraciones según la actividad deportiva.

---

# 🏆 Torneos

La aplicación permite crear y gestionar **torneos deportivos**, centralizando la información de los participantes y encuentros.

Los torneos permiten organizar múltiples partidos dentro de una misma competición y mantener la información correspondiente almacenada online.

Los datos de los torneos quedan asociados a las cuentas correspondientes, permitiendo consultar y gestionar la competición desde la aplicación.

---

# 🥇 Ligas

Equipos también permite gestionar **ligas deportivas**, orientadas a competiciones de carácter continuo.

Las ligas permiten organizar actividades donde los equipos o jugadores participan en múltiples encuentros a lo largo de una competición.

La información se mantiene centralizada en el backend para facilitar su consulta y actualización desde la aplicación.

---

# 📋 Formaciones deportivas

La aplicación incorpora un sistema para **crear, organizar y visualizar formaciones deportivas**.

Las formaciones permiten distribuir visualmente a los jugadores dentro del campo o espacio de juego.

El sistema está diseñado para adaptarse a **diferentes disciplinas deportivas**, permitiendo representar distintas configuraciones tácticas y posiciones.

Esto facilita la planificación de los equipos antes de un partido y permite visualizar la disposición de los jugadores.

---

# 🌐 Comunidad deportiva

Equipos incorpora una **comunidad deportiva geolocalizada**, cuyo objetivo es conectar usuarios, actividades y contenido deportivo según el entorno donde se encuentran.

La comunidad combina **publicaciones, ubicación geográfica, agenda y chat**, creando un espacio para descubrir y compartir actividades deportivas.

---

## 📍 Publicaciones geolocalizadas

Los usuarios pueden crear y publicar contenido dentro de la comunidad.

Cada publicación puede registrar la **ubicación geográfica del usuario en el momento en que realiza la publicación**.

Esta información permite que las publicaciones puedan ser encontradas y mostradas considerando la **zona geográfica en la que se encuentra el usuario**.

De esta manera, la comunidad puede funcionar como una red deportiva local, facilitando el descubrimiento de contenido y actividades cercanas.

Las publicaciones pueden utilizarse para compartir:

* Invitaciones a partidos.
* Convocatorias de jugadores.
* Actividades deportivas.
* Eventos.
* Información relacionada con equipos.
* Experiencias deportivas.
* Actividades disponibles en una determinada zona.

El sistema permite que el contenido tenga un contexto geográfico, haciendo que la información relevante para el usuario dependa también de su ubicación.

---

## 📅 Agenda

La comunidad incorpora una **agenda deportiva** para organizar y consultar actividades.

Los usuarios pueden utilizarla para gestionar eventos y actividades relacionados con fechas y ubicaciones determinadas.

La combinación entre agenda y geolocalización permite facilitar el descubrimiento y organización de actividades deportivas dentro del entorno del usuario.

---

## 💬 Chat en tiempo real

La aplicación incorpora un sistema de **chat mediante WebSockets**, permitiendo establecer comunicaciones en tiempo real entre los usuarios.

El uso de WebSockets permite mantener conexiones persistentes entre el dispositivo y el servidor, facilitando el intercambio de información sin depender exclusivamente de solicitudes HTTP individuales.

---

# 🔄 Sincronización online

Una característica fundamental de Equipos es la **sincronización entre el dispositivo móvil y el servidor**.

Cuando el usuario crea o modifica información desde la aplicación, los datos pueden ser enviados al backend y almacenados en PostgreSQL.

```text
             ┌──────────────┐
             │    Usuario   │
             └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │  App Equipos │
             └──────┬───────┘
                    │
              REST / WebSocket
                    │
                    ▼
             ┌──────────────┐
             │    Backend   │
             └──────┬───────┘
                    │
                    ▼
             ┌──────────────┐
             │  PostgreSQL  │
             └──────────────┘
```

Esto permite que la información permanezca centralizada y disponible online.

Entre los datos que pueden mantenerse sincronizados se encuentran:

* Usuarios.
* Jugadores.
* Equipos.
* Partidos.
* Torneos.
* Ligas.
* Formaciones.
* Publicaciones.
* Actividades de comunidad.

El objetivo es que la información no dependa exclusivamente del almacenamiento local del dispositivo.

---

# 🔐 Autenticación

El backend implementa mecanismos de autenticación para identificar a los usuarios y proteger el acceso a los recursos de la aplicación.

Se utilizan:

* **JWT (JSON Web Token)**.
* **Google OAuth**.

La autenticación permite asociar las operaciones realizadas desde la aplicación con la cuenta correspondiente y controlar el acceso a los datos.

Esto permite mantener separada la información de cada usuario dentro de la plataforma.

---

# 📡 API REST

La aplicación Android se comunica con el backend mediante una **API REST**.

La API permite realizar operaciones relacionadas con:

* Registro de usuarios.
* Inicio de sesión.
* Autenticación mediante Google.
* Gestión de usuarios.
* Gestión de jugadores.
* Gestión de partidos.
* Gestión de torneos.
* Gestión de ligas.
* Gestión de información deportiva.
* Gestión de publicaciones.
* Consulta y actualización de información.

La API constituye la principal vía de comunicación entre la aplicación móvil y el servidor.

---

# 💬 Comunicación mediante WebSockets

Además de la API REST, el backend incorpora **WebSockets** para aquellas funcionalidades que requieren comunicación en tiempo real.

Esta tecnología permite mantener una conexión persistente entre el cliente y el servidor, facilitando el intercambio inmediato de información.

Actualmente se utiliza principalmente para las funcionalidades de comunicación de la comunidad.

---

# 🗄️ Base de datos

El backend utiliza **PostgreSQL** como sistema de gestión de base de datos.

La información se almacena de manera persistente y se encuentra relacionada con las cuentas de los usuarios.

**Prisma ORM** se utiliza como capa de acceso a datos entre Node.js y PostgreSQL.

La utilización de relaciones entre las entidades permite mantener organizada la información correspondiente a usuarios, jugadores, partidos, torneos, ligas y comunidad.

---

# 🛠️ Tecnologías utilizadas

* **Node.js**
* **Express**
* **PostgreSQL**
* **Prisma ORM**
* **JWT**
* **Google OAuth**
* **WebSockets**
* **npm**

---

# 📁 Estructura del proyecto

```text
ServerEquipos/
└── server/
    ├── prisma/
    ├── scripts/
    ├── src/
    │   ├── app.js
    │   └── ws.js
    ├── package.json
    └── package-lock.json
```

---

# ⚙️ Requisitos

Para ejecutar el backend localmente se requiere:

* Node.js 20 o superior.
* PostgreSQL.
* npm.
* Git.

---

# 🚀 Instalación

Clonar el repositorio:

```bash
git clone https://github.com/Galvarine3/ServerEquipos.git
```

Ingresar al directorio del servidor:

```bash
cd ServerEquipos/server
```

Instalar las dependencias:

```bash
npm install
```

---

# 🔐 Variables de entorno

Crear un archivo `.env` con las variables necesarias para la conexión con la base de datos y los servicios de autenticación.

Ejemplo:

```env
DATABASE_URL=
JWT_SECRET=
PORT=
GOOGLE_CLIENT_ID=
APP_BASE_URL=
```

> ⚠️ No publicar contraseñas, tokens, claves de servicios ni otras credenciales sensibles en el repositorio.

---

# ▶️ Ejecución

Para iniciar el servidor en modo desarrollo:

```bash
npm run dev
```

Para ejecutar el servidor:

```bash
npm run start
```

---

# 🗃️ Prisma

Generar el cliente Prisma:

```bash
npm run prisma:generate
```

Ejecutar las migraciones:

```bash
npm run prisma:migrate
```

Prisma permite gestionar el modelo de datos y facilitar la comunicación entre el backend y PostgreSQL.

---

# ☁️ Despliegue

El backend está diseñado para funcionar como un **servicio online**, permitiendo que los dispositivos móviles se conecten remotamente mediante Internet.

En producción, el servidor puede conectarse a una instancia de PostgreSQL alojada en la nube, proporcionando almacenamiento persistente y acceso a la información de los usuarios.

La arquitectura permite que múltiples dispositivos utilicen simultáneamente el mismo backend y que cada usuario acceda a sus propios datos mediante su cuenta.

---

# 🔒 Persistencia y disponibilidad

La información asociada a cada usuario se almacena online y permanece vinculada a su cuenta.

Esto permite:

* Acceder a los datos desde diferentes dispositivos.
* Mantener la información disponible después de cerrar sesión.
* Recuperar los datos al iniciar sesión nuevamente.
* Mantener jugadores, equipos y competiciones asociados a cada cuenta.
* Sincronizar cambios entre la aplicación y el servidor.
* Compartir información de comunidad según su contexto geográfico.

---

# 📱 Resumen de funcionalidades

**Equipos** integra en una misma plataforma herramientas para la gestión y organización deportiva:

| Funcionalidad              | Descripción                                          |
| -------------------------- | ---------------------------------------------------- |
| 👤 Usuarios                | Cuentas personales con información almacenada online |
| 🏃 Jugadores               | Creación y gestión de jugadores                      |
| ⭐ Ranking                  | Jugadores con valoración para balancear encuentros   |
| ⚖️ Partidos balanceados    | Distribución automática de jugadores                 |
| 🎯 Partidos personalizados | Selección y configuración manual                     |
| 🏆 Torneos                 | Organización de competiciones                        |
| 🥇 Ligas                   | Gestión de competiciones continuas                   |
| 📋 Formaciones             | Despliegue táctico para diferentes deportes          |
| 🌐 Comunidad               | Espacio social orientado a actividades deportivas    |
| 📍 Geolocalización         | Publicaciones asociadas a la ubicación del usuario   |
| 📝 Posts                   | Publicación y descubrimiento de contenido            |
| 📅 Agenda                  | Organización de actividades y eventos                |
| 💬 Chat                    | Comunicación en tiempo real mediante WebSockets      |
| ☁️ Sincronización          | Información centralizada y disponible online         |
| 🔐 Autenticación           | JWT y Google OAuth                                   |

---

## 🎯 Objetivo

El backend de **Equipos** tiene como objetivo proporcionar una infraestructura centralizada para que la aplicación pueda combinar **gestión deportiva, organización de competiciones y comunidad**, manteniendo los datos de cada usuario sincronizados y disponibles online.

La arquitectura permite que la aplicación evolucione desde una herramienta local de organización deportiva hacia una **plataforma conectada**, donde jugadores, partidos, torneos, ligas, formaciones y actividades de la comunidad pueden gestionarse desde una cuenta personal.
