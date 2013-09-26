package com.stanfy.helium

import com.stanfy.helium.model.Field
import com.stanfy.helium.model.Message
import com.stanfy.helium.model.MethodType
import com.stanfy.helium.model.Sequence
import com.stanfy.helium.model.Note
import com.stanfy.helium.model.Project
import com.stanfy.helium.model.Service
import com.stanfy.helium.model.ServiceMethod
import com.stanfy.helium.model.StructureUnit
import com.stanfy.helium.model.Type
import groovy.transform.CompileStatic

/**
 * Utility for Helium project serialization.
 */
@CompileStatic
class HeliumWriter implements Closeable {

  // TODO: escape strings, write descriptions

  /** New line. */
  private static final String NL = "\n"

  /** Current indentation level. */
  private int indent;

  /** Output. */
  private final Writer out;

  /** Spaces buffer. */
  private String spacesBuffer = "          "

  /** Already written types. */
  private HashSet<String> writtenTypes = new HashSet<>()

  public HeliumWriter(final Writer out) {
    if (!out) { throw new IllegalArgumentException("Output is not provided") }
    this.out = out;
  }

  private String getSpaces() {
    if (!indent) { return "" }
    if (spacesBuffer.length() >= indent) {
      return spacesBuffer[0..indent-1]
    }
    StringBuilder builder = new StringBuilder(spacesBuffer)
    for (int i = spacesBuffer.length(); i < indent; i++) {
      builder << ' '
    }
    spacesBuffer = builder.toString()
    return spacesBuffer
  }

  private void incIndent() {
    indent += 2
  }
  private void decIndent() {
    indent -= 2
    if (indent < 0) {
      throw new IllegalStateException("Inconsistency detected")
    }
  }

  public void writeLine(final String str) {
    out.write getSpaces()
    out.write str
    out.write NL
  }

  void writeProject(final Project project) throws IOException {
    project.structure.each { StructureUnit unit ->
      if (unit instanceof Note) {
        writeNote((Note) unit)
      } else if (unit instanceof Type) {
        writeType((Type) unit)
      } else if (unit instanceof Service) {
        writeService((Service) unit)
      }
    }
  }

  void writeNote(final Note note) throws IOException {
    writeLine "note '''"
    incIndent()
    note.lines.each { String noteLine -> writeLine noteLine }
    decIndent()
    writeLine "'''"
  }

  void writeMessage(final Message message) throws IOException {
    startMessage(message.name)
    writeMessageFields(message)
    endMessage()
  }

  void startMessage(final String name) throws IOException {
    writtenTypes.add name
    writeLine "type '$name' message {"
    incIndent()
  }

  void endMessage() throws IOException {
    decIndent()
    writeLine("}")
  }

  void emitField(final Field field) throws IOException {
    if (!field.type) {
      throw new IllegalStateException("Type of the field '$field.name' is not specified")
    }
    writeLine "$field.name {"
    incIndent()
    writeLine "type '${field.type.name}'"
    writeLine "required $field.required"
    if (field.sequence) {
      writeLine "sequence $field.sequence"
    }
    if (field.examples) {
      // TODO
      writeLine "examples $field.examples"
    }
    decIndent()
    writeLine "}"
  }

  void writeSequence(final Sequence seq) throws IOException {
    writtenTypes.add seq.name
    writeLine "type '$seq.name' sequence '$seq.itemsType.name'"
  }

  void writeType(final Type type) throws IOException {
    if (type instanceof Message) {
      writeMessage((Message) type)
    } else if (type instanceof Sequence) {
      writeSequence((Sequence)type)
    } else {
      writtenTypes.add type.name
      writeLine "type '${type.name}'"
    }
  }

  void writeService(final Service service) throws IOException {
    startService()
    writeLine "name '$service.name'"
    writeLine "version '$service.version'"
    writeLine "location '$service.location'"
    if (service.encoding) {
      writeLine "encoding $service.encoding"
    }
    service.methods.each { ServiceMethod m -> writeServiceMethod(m) }
    endService()
  }

  void startService() throws IOException {
    writeLine "service {"
    incIndent()
  }

  void endService() throws IOException {
    decIndent()
    writeLine "}"
  }

  void writeServiceMethod(final ServiceMethod method) throws IOException {
    startServiceMethod(method.path, method.type)
    writeLine "name '$method.name'"
    if (method.encoding) {
      writeLine "encoding $method.encoding"
    }
    if (method.parameters) {
      emitMethodInternalType("parameters", method.parameters)
    }
    if (method.body) {
      emitMethodInternalType("body", method.body)
    }
    if (method.response) {
      emitMethodInternalType("response", method.response)
    }
    endServiceMethod()
  }

  void startServiceMethod(final String path, final MethodType type) {
    writeLine "${type.toString().toLowerCase()} '$path' spec {"
    incIndent()
  }

  void endServiceMethod() throws IOException {
    decIndent()
    writeLine "}"
  }

  private void emitMethodInternalType(final String property, final Type type) throws IOException {
    if (writtenTypes.contains(type.name)) {
      writeLine "$property '$type.name'"
      return
    }
    if (type instanceof Message) {
      writeLine "$property {"
      incIndent()
      writeMessageFields((Message)type)
      decIndent()
      writeLine "}"
      return
    }
    throw new IllegalStateException("$type was not defined earlier")
  }

  private void writeMessageFields(final Message message) throws IOException {
    message.fields.each { Field f ->
      emitField(f)
    }
  }

  @Override
  void close() throws IOException {
    out.close()
  }

}