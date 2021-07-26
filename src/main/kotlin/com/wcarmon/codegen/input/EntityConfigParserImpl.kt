package com.wcarmon.codegen.input

import com.fasterxml.jackson.databind.ObjectReader
import com.wcarmon.codegen.model.Entity
import java.nio.file.Path


class EntityConfigParserImpl(
  private val objectReader: ObjectReader,
) : EntityConfigParser {

  override fun parse(entityConfigs: Collection<Path>): Collection<Entity> {
    require(entityConfigs.isNotEmpty()) {
      "at least one entity config is required"
    }

    //TODO: Add linker for entity relationships

    return entityConfigs.map { parse(it) }
  }

  private fun parse(entityConfigFile: Path): Entity {

    //TODO: do schema validation here
//    val schemaFactory: JsonSchemaFactory =
//      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)
//
//    val schema: JsonSchema = schemaFactory.getSchema(schemaStream)
//    val validationResult: Set<ValidationMessage> = schema.validate(json)

    val parsed = objectReader.readValue(
      entityConfigFile.toFile(),
      Entity::class.java)

    return parsed
  }
}
