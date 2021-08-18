package uk.gov.ons.addressIndex.server.modules.presetClassificationFilters

import com.sksamuel.elastic4s.requests.searches.queries.PrefixQuery
import org.scalatest._
import flatspec._
import matchers._
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class EducationalClassificationFilterSpec extends AnyWordSpec with should.Matchers {

  "EducationalClassificationFilter" should {

    "return a CE prefix query" in {

      // Given
      val expected = Seq(PrefixQuery("classificationCode", "CE"))

      // When
      val result = EducationalClassificationFilter.queryFilter

      // Then
      result shouldBe expected
    }
  }
}