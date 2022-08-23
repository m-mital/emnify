package com.emnify.common

abstract class Fail extends Exception

object Fail {
  case class NotFound(what: String) extends Fail
  case class Conflict(msg: String) extends Fail
  case class IncorrectInput(msg: String) extends Fail
}
