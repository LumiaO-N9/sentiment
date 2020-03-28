from sendgrid import SendGridAPIClient
from sendgrid.helpers.mail import Mail
from settings import SENDGRID_APIKEY

message = Mail(
    from_email='xiaoBei@sentiment.com',
    to_emails='1044740758@qq.com',
    subject='Sending with Twilio SendGrid is Fun',
    html_content='<strong>and easy to do anywhere, even with Python</strong>')
try:
    sg = SendGridAPIClient(SENDGRID_APIKEY)
    response = sg.send(message)
    print(response.status_code)
    print(response.body)
    print(response.headers)
except Exception as e:
    print(e.message)
