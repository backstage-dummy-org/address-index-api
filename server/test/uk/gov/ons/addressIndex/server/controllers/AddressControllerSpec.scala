package uk.gov.ons.addressIndex.server.controllers

import uk.gov.ons.addressIndex.model.server.response._
import uk.gov.ons.addressIndex.server.modules.{AddressIndexCannedResponse, AddressParserModule, ElasticsearchRepository}
import com.sksamuel.elastic4s.ElasticClient
import org.elasticsearch.common.settings.Settings
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results
import play.api.test.FakeRequest
import uk.gov.ons.addressIndex.model.db.index.{NationalAddressGazetteerAddress, NationalAddressGazetteerAddresses, PostcodeAddressFileAddress, PostcodeAddressFileAddresses}
import org.scalatestplus.play._
import play.api.test.Helpers._
import uk.gov.ons.addressIndex.crfscala.CrfScala.CrfTokenResult

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddressControllerSpec extends PlaySpec with Results with AddressIndexCannedResponse {

  val validPafAddress = PostcodeAddressFileAddress(
    recordIdentifier = "1",
    changeType = "2",
    proOrder = "3",
    uprn = "4",
    udprn = "5",
    organizationName = "6",
    departmentName = "7",
    subBuildingName = "8",
    buildingName = "9",
    buildingNumber = "10",
    dependentThoroughfare = "11",
    thoroughfare = "12",
    doubleDependentLocality = "13",
    dependentLocality = "14",
    postTown = "15",
    postcode = "B16 8TH",
    postcodeType = "17",
    deliveryPointSuffix = "18",
    welshDependentThoroughfare = "19",
    welshThoroughfare = "20",
    welshDoubleDependentLocality = "21",
    welshDependentLocality = "22",
    welshPostTown = "23",
    poBoxNumber = "24",
    processDate = "25",
    startDate = "26",
    endDate = "27",
    lastUpdateDate = "28",
    entryDate = "29",
    score = 1.0f
  )

  val validNagAddress = NationalAddressGazetteerAddress(
    uprn = "1",
    postcodeLocator = "B16 8TH",
    addressBasePostal = "3",
    latitude = "24",
    longitude = "25",
    easting = "27",
    northing = "28",
    organisation = "22",
    legalName = "23",
    classificationCode = "29",
    usrn = "4",
    lpiKey = "5",
    paoText = "6",
    paoStartNumber = "72",
    paoStartSuffix = "8",
    paoEndNumber = "9",
    paoEndSuffix = "10",
    saoText = "11",
    saoStartNumber = "12",
    saoStartSuffix = "13",
    saoEndNumber = "14",
    saoEndSuffix = "15",
    level = "16",
    officialFlag = "17",
    logicalStatus = "18",
    streetDescriptor = "19",
    townName = "20",
    locality = "21",
    score = 1.0f
  )

  // injected value, change implementations accordingly when needed
  // mock that will return one address as a result
  val elasticRepositoryMock = new ElasticsearchRepository {

    override def queryPafUprn(uprn: String): Future[Option[PostcodeAddressFileAddress]] =
      Future.successful(Some(validPafAddress))

    override def queryNagUprn(uprn: String): Future[Option[NationalAddressGazetteerAddress]] =
      Future.successful(Some(validNagAddress))

    override def queryPafAddresses(tokens: Seq[CrfTokenResult]): Future[PostcodeAddressFileAddresses] =
      Future.successful(PostcodeAddressFileAddresses(Seq(validPafAddress), 1.0f))

    override def queryNagAddresses(tokens: Seq[CrfTokenResult]): Future[NationalAddressGazetteerAddresses] =
      Future.successful(NationalAddressGazetteerAddresses(Seq(validNagAddress), 1.0f))

    override def client(): ElasticClient = ElasticClient.local(Settings.builder().build())
  }

  // mock that won't return any addresses
  val emptyElasticRepositoryMock = new ElasticsearchRepository {

    override def queryPafUprn(uprn: String): Future[Option[PostcodeAddressFileAddress]] = Future.successful(None)

    override def queryNagUprn(uprn: String): Future[Option[NationalAddressGazetteerAddress]] = Future.successful(None)

    def queryPafAddresses(tokens: Seq[CrfTokenResult]): Future[PostcodeAddressFileAddresses] =
      Future.successful(PostcodeAddressFileAddresses(Seq.empty, 1.0f))

    def queryNagAddresses(tokens: Seq[CrfTokenResult]): Future[NationalAddressGazetteerAddresses] =
      Future.successful(NationalAddressGazetteerAddresses(Seq.empty, 1.0f))

    override def client(): ElasticClient = ElasticClient.local(Settings.builder().build())
  }

  val parser = new AddressParserModule

  def testController: AddressController = new AddressController(elasticRepositoryMock, parser)

  "Address controller" should {

    "reply with a found PAF address (by address query)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressBySearchResponseContainer(
        AddressBySearchResponse(
          tokens = Seq.empty,
//            AddressTokens(
//            uprn = "",
//            buildingNumber = "10",
//            postcode = "B16 8TH"
//          ),
          addresses = Seq(AddressResponseAddress.fromPafAddress(1.0f)(validPafAddress)),
          limit = 10,
          offset = 0,
          total = 1
        ),
        OkAddressResponseStatus
      ))

      // When
      val result = controller.addressQuery("10 B16 8TH", "paf").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe OK
      actual mustBe expected
    }

    "reply with a found NAG address (by address query)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressBySearchResponseContainer(
        AddressBySearchResponse(
          tokens = Seq.empty,
//          AddressTokens(
//            uprn = "",
//            buildingNumber = "72",
//            postcode = "B16 8TH"
//          ),
          addresses = Seq(AddressResponseAddress.fromNagAddress(1.0f)(validNagAddress)),
          limit = 10,
          offset = 0,
          total = 1
        ),
        OkAddressResponseStatus
      ))

      // When
      val result = controller.addressQuery("72 B16 8TH", "bs").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe Ok
      actual mustBe expected
    }

    "reply on a 400 error if address format is not supported (by address query)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressBySearchResponseContainer(
        AddressBySearchResponse(
          tokens = Seq.empty,
          addresses = Seq.empty,
          limit = 10,
          offset = 0,
          total = 0
        ),
        BadRequestAddressResponseStatus,
        errors = Seq(FormatNotSupportedAddressResponseError)
      ))

      // When
      val result = controller.addressQuery("10 B16 8TH", "format is not supported").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe BadRequest
      actual mustBe expected
    }

    "reply on a 400 error if query is empty (by address query)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressBySearchResponseContainer(
        AddressBySearchResponse(
          tokens = Seq.empty,
          addresses = Seq.empty,
          limit = 10,
          offset = 0,
          total = 0
        ),
        BadRequestAddressResponseStatus,
        errors = Seq(EmptyQueryAddressResponseError)
      ))

      // When
      val result = controller.addressQuery("", "paf").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe BadRequest
      actual mustBe expected
    }

    "reply on a found PAF address (by uprn)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressByUprnResponseContainer(
        response = AddressByUprnResponse(
          address = Some(AddressResponseAddress.fromPafAddress(validPafAddress))
        ),
        OkAddressResponseStatus
      ))

      // When
      val result = controller.uprnQuery("4", "paf").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe Ok
      actual mustBe expected
    }

    "reply on a found NAG address (by uprn)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressByUprnResponseContainer(
        response = AddressByUprnResponse(
          address = Some(AddressResponseAddress.fromNagAddress(validNagAddress))
        ),
        OkAddressResponseStatus
      ))

      // When
      val result = controller.uprnQuery("1", "bs").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe Ok
      actual mustBe expected
    }

    "reply a 404 error if address was not found (by uprn)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressByUprnResponseContainer(
        response = AddressByUprnResponse(
          address = None
        ),
        NotFoundAddressResponseStatus,
        errors = Seq(NotFoundAddressResponseError)
      ))

      // When
      val result = controller.uprnQuery("doesn't exist", "paf").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe NotFound
      actual mustBe expected
    }

    "reply a 400 error if address format is not supported (by uprn)" ignore {
      // Given
      val controller = testController

      val expected = Json.toJson(AddressByUprnResponseContainer(
        response = AddressByUprnResponse(
          address = None
        ),
        BadRequestAddressResponseStatus,
        errors = Seq(FormatNotSupportedAddressResponseError)
      ))

      // When
      val result = controller.uprnQuery("4", "format is not supported").apply(FakeRequest())
      val actual: JsValue = contentAsJson(result)

      // Then
      status(result) mustBe BadRequest
      actual mustBe expected
    }

  }
}
