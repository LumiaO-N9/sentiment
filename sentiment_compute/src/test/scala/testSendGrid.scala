import com.sendgrid._
import java.io.IOException
import com.shujia.Constant

object testSendGrid extends App {

  val from = new Email("xiaoBei@sentiment.com")
  val subject = "Sending with SendGrid is Fun"
  val to = new Email("1044740758@qq.com")
  val content = new Content("text/plain", "and easy to do anywhere, even with Java")
  val mail = new Mail(from, subject, to, content)

  val sg = new SendGrid(Constant.SENDGRID_APIKEY)
  val request = new Request()
  try {
    request.setMethod(Method.POST)
    request.setEndpoint("mail/send")
    request.setBody(mail.build)
    val response = sg.api(request)
    System.out.println(response.getStatusCode)
    System.out.println(response.getBody)
    System.out.println(response.getHeaders)
  } catch {
    case ex: IOException =>
      throw ex
  }
}
