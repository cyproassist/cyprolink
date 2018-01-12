package de.cyproassist.web.util

import net.enilink.komma.core.URIs

object DCTERMS {
  val NS_URI = URIs.createURI("http://purl.org/dc/terms/")

  val PROPERTY_DATE = NS_URI.appendLocalPart("date")
}

object LF_MAINT {
  val NS_URI = URIs.createURI("http://linkedfactory.org/vocab/maintenance#")

  val TYPE_EVENT = NS_URI.appendLocalPart("Event")
  
  val PROPERTY_NR = NS_URI.appendLocalPart("nr")
  val PROPERTY_OCCURSINLOCATION = NS_URI.appendLocalPart("occursInLocation")
  val PROPERTY_OCCURSINMACHINE = NS_URI.appendLocalPart("occursInMachine")
}