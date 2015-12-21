package com.stanfy.helium.handler.codegen.objectivec.entity.builder

import com.stanfy.helium.handler.codegen.objectivec.entity.ObjCEntitiesOptions
import com.stanfy.helium.handler.codegen.objectivec.entity.classtree.ObjCProjectClassesStructure
import com.stanfy.helium.handler.codegen.objectivec.entity.filetree.ObjCHeaderFile
import com.stanfy.helium.handler.codegen.objectivec.entity.filetree.ObjCImplementationFile
import com.stanfy.helium.handler.codegen.objectivec.entity.filetree.ObjCProjectFilesStructure

/**
 * Created by paultaykalo on 12/17/15.
 */

class ObjCDefaultFileStructureBuilder : ObjCFileStructureBuilder {

  override fun build(from: ObjCProjectClassesStructure): ObjCProjectFilesStructure {
    return this.build(from, null)
  }

  override fun build(from: ObjCProjectClassesStructure, options: ObjCEntitiesOptions?): ObjCProjectFilesStructure {
    val result = ObjCProjectFilesStructure()
    from.classes.forEach { objcClass ->
      var headerBuilder =ObjCHeaderFileBuilder()
      val headerFile = ObjCHeaderFile(objcClass.name, headerBuilder.build(objcClass,options))
      val implementationFile = ObjCImplementationFile(objcClass.name, objcClass.implementation.asString())
      result.addFile(headerFile)
      result.addFile(implementationFile)
    }
    from.pregeneratedClasses.forEach { objcClass ->
      if (objcClass.header != null) {
        val headerFile = ObjCHeaderFile(objcClass.name, objcClass.header)
        result.addFile(headerFile)
      }
      if (objcClass.implementation != null) {
        val implementationFile = ObjCImplementationFile(objcClass.name, objcClass.implementation)
        result.addFile(implementationFile)
      }
    }

    return result
  }


}