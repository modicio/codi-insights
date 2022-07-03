/*
 * This file is part of the codi-insights software.
 * Copyright (C) 2022 Karl Kegel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * */

package env

import codi.core.{InstanceFactory, Registry, TypeFactory}
import codi.core.rules.Rule
import codi.nativelang.input.{NativeInput, NativeInputParser, NativeInputTransformator}
import codi.nativelang.logic.{SimpleDefinitionVerifier, SimpleMapRegistry, SimpleModelVerifier}
import modules.meta.NamedElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

object RegistryProvider {

  val modelVerifier = new SimpleModelVerifier()
  val definitionVerifier = new SimpleDefinitionVerifier()

  val typeFactory = new TypeFactory(definitionVerifier, modelVerifier)
  val instanceFactory = new InstanceFactory(definitionVerifier, modelVerifier)

  private var registry: Option[Registry] = None

  private def init(): Future[Unit] = {

    //Rules generate their own UUIDs:ss
    Rule.enableAutoID()

    registry = Some(new SimpleMapRegistry(typeFactory, instanceFactory))

    typeFactory.setRegistry(registry.get)
    instanceFactory.setRegistry(registry.get)

    val source = Source.fromFile("resources/json_hybrid_rule_example.json")
    val fileContents = source.getLines.mkString
    println(fileContents)
    source.close()

    val initialInput: NativeInput = NativeInputParser.parse(fileContents)
    val transformator: NativeInputTransformator = new NativeInputTransformator(registry.get, definitionVerifier, modelVerifier)

    for{
      _ <- new NamedElement().setup(registry.get, definitionVerifier)
      _ <- transformator.extendModel(initialInput)
    } yield Future.successful()
  }

  def getRegistry: Future[Registry] = {
    if(registry.isDefined){
      Future.successful(registry.get)
    }else{
      init() map (_ => registry.get)
    }

  }

}