-- Migracion para PostgreSQL (perfil prod, ddl-auto=validate).
-- 1. Eliminacion logica de pools, lanes y correlaciones (@SQLDelete + @SQLRestriction, columna activo).
-- 2. Una correlacion eliminada sigue en la tabla: mensaje_id deja de ser unico.
-- 3. El correo del usuario pasa a ser unico en todo el sistema (antes por empresa).
-- 4. nodos_flujo.contenido_evento de OID (@Lob) a TEXT, igual que V1 y V2.
--
-- Ejecutar ANTES de reiniciar la aplicacion con estos cambios.

ALTER TABLE pools ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE lanes ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;
ALTER TABLE correlaciones ADD COLUMN IF NOT EXISTS activo boolean NOT NULL DEFAULT true;

DO $$
DECLARE restriccion record;
BEGIN
    FOR restriccion IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class tabla ON tabla.oid = con.conrelid
        WHERE tabla.relname = 'correlaciones' AND con.contype = 'u'
    LOOP
        EXECUTE format('ALTER TABLE correlaciones DROP CONSTRAINT %I', restriccion.conname);
    END LOOP;
END $$;

ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_email UNIQUE (email);

ALTER TABLE nodos_flujo ADD COLUMN contenido_evento_tmp text;
UPDATE nodos_flujo SET contenido_evento_tmp = convert_from(lo_get(contenido_evento), 'UTF8')
WHERE contenido_evento IS NOT NULL;
ALTER TABLE nodos_flujo DROP COLUMN contenido_evento;
ALTER TABLE nodos_flujo RENAME COLUMN contenido_evento_tmp TO contenido_evento;
