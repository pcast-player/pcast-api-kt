package io.pcast.helpers

import com.fasterxml.uuid.Generators
import java.util.UUID

fun generateUuidV7(): UUID = Generators.timeBasedEpochGenerator().generate()