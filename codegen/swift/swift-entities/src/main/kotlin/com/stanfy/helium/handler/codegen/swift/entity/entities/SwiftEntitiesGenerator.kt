package com.stanfy.helium.handler.codegen.swift.entity.entities

import com.stanfy.helium.internal.utils.Names
import com.stanfy.helium.model.Field
import com.stanfy.helium.model.Project
import com.stanfy.helium.model.Sequence
import com.stanfy.helium.model.Type
import com.stanfy.helium.model.constraints.ConstrainedType
import com.stanfy.helium.model.constraints.EnumConstraint

interface SwiftEntitiesGenerator {
  fun entitiesFromHeliumProject(project: Project): List<SwiftEntity>
  fun entitiesFromHeliumProject(project: Project, customTypesMappings: Map<String, String>?): List<SwiftEntity>
  fun entitiesFromHeliumProject(project: Project, customTypesMappings: Map<String, String>?, defaultValues: Map<String, String>?): List<SwiftEntity>
  fun swiftType(heliumType: Type, registry: MutableMap<String, SwiftEntity>): SwiftEntity
}

class SwiftEntitiesGeneratorImpl : SwiftEntitiesGenerator {
  override fun entitiesFromHeliumProject(project: Project): List<SwiftEntity>{
    return entitiesFromHeliumProject(project, null, null)
  }

  override fun entitiesFromHeliumProject(project: Project, customTypesMappings: Map<String, String>?): List<SwiftEntity> {
    return entitiesFromHeliumProject(project, customTypesMappings, null)
  }

  override fun entitiesFromHeliumProject(project: Project, customTypesMappings: Map<String, String>?, defaultValues: Map<String, String>?): List<SwiftEntity> {
    val typesRegistry: MutableMap<String, SwiftEntity> = hashMapOf()
    if (customTypesMappings != null) {
      typesRegistry.putAll(customTypesMappings.mapValues { name -> SwiftEntityStruct(name.value) })
    }

    val enumsWithTypes = project.types.all()
        .filterIsInstance<ConstrainedType>()
        .map { type ->
          val enumType = enumType(type)
          if (enumType != null) Pair(type, enumType) else null
        }
        .filterNotNull()

    enumsWithTypes.forEach { typeEnum ->
      typesRegistry.put(typeEnum.first.name, typeEnum.second)
    }

    val enums = enumsWithTypes.map { typeEnum ->
      typeEnum.second
    }

    val messages = project.messages
        .filterNot { message -> message.isAnonymous }
        .map { message ->
          val props = message.fields
              .filterNot { field -> field.isSkip }
              .map { field ->
                val type = if (field.isSequence) simpleSequenceType(field.type, typesRegistry) else swiftType(field.type, typesRegistry)
                val hasDefaultValue = defaultValues?.contains(field.type.name) ?: false
                val fieldType = if (field.isRequired || hasDefaultValue) type else type.toOptional()
                SwiftProperty(propertyName(field.name), fieldType, field.name)
              }
          SwiftEntityStruct(message.name, props)
        }

    val sequences = project.sequences
        .filterNot { sequence -> sequence.isAnonymous }
        .map { sequence ->
          swiftType(sequence, typesRegistry)
        }


    return enums + messages + sequences
  }

  fun propertyName(fieldName: String): String {
    val prettifiedName = Names.prettifiedName(Names.canonicalName(fieldName))
    if (arrayOf("enum", "default", "let", "case").contains(prettifiedName)) {
      return prettifiedName + "Value"
    }
    return prettifiedName
  }

  override fun swiftType(heliumType: Type, registry: MutableMap<String, SwiftEntity>): SwiftEntity {
    return registry.getOrElse(heliumType.name) {
      val type: SwiftEntity =
          trySequenceType(heliumType, registry)
              ?: tryPrimitiveType(heliumType)
              ?: structType(heliumType)
      registry.put(heliumType.name, type)
      return type
    }
  }

  private fun trySequenceType(heliumType: Type, registry: MutableMap<String, SwiftEntity>): SwiftEntityArray? {
    if (heliumType !is Sequence) return null
    return SwiftEntityArray(heliumType.name, swiftType(heliumType.itemsType, registry))
  }

  private fun simpleSequenceType(heliumType: Type, registry: MutableMap<String, SwiftEntity>): SwiftEntityArray {
    return SwiftEntityArray("", swiftType(heliumType, registry))
  }

  private fun structType(heliumType: Type): SwiftEntityStruct {
    return SwiftEntityStruct(heliumType.name)
  }

  private fun tryPrimitiveType(heliumType: Type): SwiftEntityPrimitive? {
    return when (heliumType.name) {
      "int" -> SwiftEntityPrimitive("Int")
      "integer" -> SwiftEntityPrimitive("Int")
      "int32" -> SwiftEntityPrimitive("Int")
      "int64" -> SwiftEntityPrimitive("Int")
      "long" -> SwiftEntityPrimitive("Int")
      "double" -> SwiftEntityPrimitive("Double")
      "float" -> SwiftEntityPrimitive("Double")
      "float32" -> SwiftEntityPrimitive("Double")
      "float64" -> SwiftEntityPrimitive("Double")
      "string" -> SwiftEntityPrimitive("String")
      "bool" -> SwiftEntityPrimitive("Bool")
      "boolean" -> SwiftEntityPrimitive("Bool")
      else -> {
        null
      }
    }
  }

  private fun enumType(heliumType: Type): SwiftEntityEnum? {
    if (heliumType !is ConstrainedType) return null
    val constraint = heliumType.constraints.first { con -> con is EnumConstraint } as? EnumConstraint<Any> ?: return null
    val enumValues = constraint.values
        .filterIsInstance<String>()
        .map { s ->
          SwiftEntityEnumCase(
              name = propertyName(s).capitalize(),
              value = s)
        }
    return SwiftEntityEnum(propertyName(heliumType.name).capitalize(), enumValues)

  }
}