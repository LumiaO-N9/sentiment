from datetime import datetime
import random

import requests

'''
#3C8D40
#ff4d4f
#1890ff
'''
id = "2"
json_dict = requests.get("http://localhost:8080/getRealTimeSentiment?id=%s" % id).json()

xLine = json_dict["x"]
y1Line = json_dict["y1"]
y2Line = json_dict["y2"]
y3Line = json_dict["y3"]


def transTime(timeStr):
    return datetime.strptime(timeStr, "%Y-%m-%d %H").strftime("%Y/%m/%d %H:%M:%S")


def counstructSentimentList(yList, category):
    listTemp = []
    for i in range(0, 100):
        dictTemp = {}
        dictTemp["x"] = transTime(xLine[i])
        dictTemp["y"] = yList[i]
        dictTemp["s"] = category
        listTemp.append(dictTemp)
    return listTemp


outputList = counstructSentimentList(y1Line, "1") + counstructSentimentList(y2Line, "2") + counstructSentimentList(
    y3Line, "3")
print(str(outputList).replace("'", '"'))

gender_list = requests.get("http://localhost:8080/getGenderCount?id=%s" % id).json()
print(str(gender_list).replace("'", '"'))

followers_dict = requests.get("http://localhost:8080/getFollowersCount?id=%s" % id).json()
print(followers_dict)
xLine1 = followers_dict['x']
yLine1 = followers_dict['y']
followers_list = []
for i in range(0, 10):
    dictTemp = {}
    dictTemp['x'] = xLine1[i]
    dictTemp['y'] = yLine1[i]
    followers_list.append(dictTemp)
print(str(followers_list).replace("'", '"'))

r = requests.get("http://localhost:8080/getWordCloud?id=%s" % id)
word_source_list = r.json()
word_list = []
for i in range(0, len(word_source_list)):
    dictTemp = {}
    dictTemp = word_source_list[i]
    dictTemp["type"] = random.randint(1, 7)
    word_list.append(dictTemp)

print(str(word_list).replace("'", '"'))
