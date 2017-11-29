package io.github.agaro1121.xmasservice.models

trait PresentType
case object Physical extends PresentType
case object Virtual extends PresentType

case class XmasPresent(name: String, presentType: PresentType)
