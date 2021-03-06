package com.wcarmon.codegen

import com.fasterxml.jackson.databind.ObjectReader
import com.wcarmon.codegen.input.EntityConfigParser
import com.wcarmon.codegen.input.getPathsMatchingNamePattern
import com.wcarmon.codegen.log.structuredInfo
import com.wcarmon.codegen.model.CodeGenRequest
import com.wcarmon.codegen.model.Entity
import com.wcarmon.codegen.model.OutputMode.SINGLE_FILE
import org.apache.commons.lang3.StringUtils
import org.apache.logging.log4j.LogManager
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Path

@Configuration
@EnableAutoConfiguration
class CodeGeneratorApp(
  private val entityConfigParser: EntityConfigParser,
  private val generator: CodeGenerator,
  private val objectReader: ObjectReader,
  private val templateFinder: (Path) -> freemarker.template.Template,
) {

  companion object {
    @JvmStatic
    private val LOG = LogManager.getLogger(CodeGeneratorApp::class.java)
  }

  /**
   * @param requestConfigRoot where to start looking for request.json files
   *
   * See [PATTERN_FOR_GEN_REQ_FILE]
   */
  fun run(requestConfigRoot: Path) {
    val cleanRoot = requestConfigRoot.normalize().toAbsolutePath()

    require(Files.exists(cleanRoot)) { "configRoot must exist at $cleanRoot" }
    require(Files.isDirectory(cleanRoot)) { "expected directory at $cleanRoot" }

    val paths = findCodeGenRequestFiles(cleanRoot)
    LOG.structuredInfo(
      "Found Code Generate requests",
      "count" to paths.size,
      "files" to StringUtils.truncate(paths.toString(), 256),
    )

    val requests = parseCodeGenRequests(paths)
    check(requests.isNotEmpty()) {
      "Cannot find any codegen requests under $cleanRoot"
    }

    requests
      .forEach { codeGenRequest ->

        val entities = findEntityConfigs(codeGenRequest.entityConfigDirs)

        val names = StringUtils.truncate(
          entities
            .map { it.name.upperCamel }
            .sortedBy { it }
            .joinToString(),
          256)

        LOG.structuredInfo(
          "Found entity configs for request",
          "entityCount" to entities.size,
          "names" to names,
          "template" to codeGenRequest.template,
        )

        // -- Enforce unique entity names
        val entityNames = entities.map { it.name.lowerCamel }
        val duplicateEntityNames =
          entityNames
            .groupBy { it }
            .filter { it.value.size > 1 }
            .map { it.key }

        require(duplicateEntityNames.isEmpty()) {
          "Entity names must be unique: duplicates=${duplicateEntityNames}"
        }

        handleCodeGenRequest(codeGenRequest, entities)
      }
  }

  private fun handleCodeGenRequest(
    request: CodeGenRequest,
    entities: Collection<Entity>,
  ) {
    val template = templateFinder(request.template.file.toPath())

    Files.createDirectories(request.cleanOutputDir)

    if (request.outputMode == SINGLE_FILE) {
      generator.generateOneFileForEntities(
        entities = entities,
        request = request,
        template = template,
      )

      return
    }

    // Invariant: Generating multiple files

    require(request.outputFilenameTemplate.isNotBlank()) {
      "outputFilenameTemplate required when generating multiple files"
    }

    val fileNameBuilder = { entity: Entity ->
      val entityNameInFile = entity.name.forCaseFormat(
        request.caseFormatForOutputFile
      )

      String.format(
        request.outputFilenameTemplate,
        entityNameInFile
      )
    }

    generator.generateFilePerEntity(
      entities = entities,
      fileNameBuilder = fileNameBuilder,
      request = request,
      template = template,
    )
  }

  /**
   * @return parsed [CodeGenRequest] instances
   */
  private fun parseCodeGenRequests(files: Collection<Path>): Collection<CodeGenRequest> =
    files.map { path ->
      try {
        //TODO: validate via json-schema here

        objectReader.readValue(
          path.toFile(),
          CodeGenRequest::class.java
        )

      } catch (ex: Exception) {
        throw RuntimeException("Failed to parse codegen request json file: path=$path", ex)
      }
    }

  /**
   * Traverse `configRoot`,
   * parse found json files to [CodeGenRequest] instances
   *
   * @param configRoot file system root for searching
   * @return paths matching the pattern
   *
   * See [PATTERN_FOR_GEN_REQ_FILE]
   */
  private fun findCodeGenRequestFiles(configRoot: Path): Collection<Path> {

    val generatorRequestPaths = getPathsMatchingNamePattern(
      pathPattern = PATTERN_FOR_GEN_REQ_FILE,
      searchRoot = configRoot,
    )

    require(generatorRequestPaths.isNotEmpty()) {
      "At least one Code Generate request file is required under $configRoot"
    }

    return generatorRequestPaths
  }

  /**
   * Traverse each root in `configRoots`,
   * parse found json files to [Entity] instances
   *
   * @param configRoots file system roots for searching
   * @return parsed [Entity] instances
   *
   * See [PATTERN_FOR_ENTITY_FILE]
   */
  private fun findEntityConfigs(configRoots: Collection<Path>): Collection<Entity> {
    require(configRoots.isNotEmpty()) { "At least one configRoot is required" }

    val entityConfigPaths = configRoots.flatMap { configRoot ->
      getPathsMatchingNamePattern(
        pathPattern = PATTERN_FOR_ENTITY_FILE,
        searchRoot = configRoot,
      )
    }.toSet()

    require(entityConfigPaths.isNotEmpty()) {
      "At least one entity config file is required under configRoots=$configRoots"
    }

    return entityConfigParser
      .parse(entityConfigPaths)
      .sortedBy { it.name.upperCamel }
  }
}
