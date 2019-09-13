# PlusPeter Back End Tech Test

The purpose of this test is to assess your ability to create a simple service which consumes data from some backing services and returns a result by composing that data together.

Our chosen back end language is Go but it is OK to submit the solution to this test in another language if you are more familiar with it.
We have skills in house to make sense of Go, Python, Node.js, Ruby, Java, PHP or Elixir. If you want to use something not on that list please ask first.

The test will require you to write a very simple service (described below), a set of tests for said service and Docker containers for that service and its tests, which will be run using Docker compose.

[You can learn more about docker-compose here](https://docs.docker.com/compose/)

If you are running Windows or Mac OS you can install [Docker For Windows](https://docs.docker.com/docker-for-windows/) or [Docker For Mac](https://docs.docker.com/docker-for-mac/)

## The Backing Services
There are two backing services, temperature and windspeed. Both work in a very similar way

The temperature service will return an average temperature in degrees Celcius for any given date between Jan 1 1900 and today. The temperature given is random and nonsensical *but it is consistent*
The windspeed service will return an average wind speed in meters per second for any given date between Jan 1 1900 and today. The windspeed given is random and nonsensical *but it is consistent*

Each service can only return one temperature or date per request

For example, if I were to start the backing services by changing directory to this folder and running
```bash
docker-compose up
```

Then once the services are started I could use curl to get the temperature for 12th August 2018 with:
```bash
curl "http://localhost:8000/?at=2018-08-12T12:00:00Z"
```

and I should get a result which looks like
```json
{"temp":10.46941232124016,"date":"2018-08-12T00:00:00Z"}
```

I can get the windspeed for the same date with

```bash
curl "http://localhost:8080/?at=2018-08-12T12:00:00Z"
```

and I'll get a result which looks like
```bash
{"north":-17.989980201472466,"west":16.300917971882726,"date":"2018-08-12T00:00:00Z"}
```

The "at" parameter must be an ISO8601 DateTime.

## The service

You must develop a service which exposes the following 3 api methods

1: `GET /temperatures?start=2018-08-01T00:00:00Z&end=2018-08-07T00:00:00Z`

This method will return each daily temperature beginning at the start date and ending at but including the the end date.
The resulting temperatures should be in the correct order
The parameter start should be an ISO8601 DateTime. The parameter end should be an ISO8601 DateTime.
The dates given are just examples. The method should work for any range of dates provided by the backing services. If any date is not available then the service should return an appropriate error with a JSON formatted error message

The response should be formatted as JSON and should look something like the following:
```json
[
  {
    "temp": 10.46941232124016,
    "date": "2018-08-01T00:00:00Z"
  },
  {
    "temp": 13.5353456555445,
    "date": "2018-08-02T00:00:00Z"
  },
  {
    "temp": 8.23423423423344,
    "date": "2018-08-03T00:00:00Z"
  },
  {
    "temp": 11.6456546546454,
    "date": "2018-08-04T00:00:00Z"
  },
  {
    "temp": 5.879879879879889,
    "date": "2018-08-05T00:00:00Z"
  },
  {
    "temp": 15.34354353454353,
    "date": "2018-08-06T00:00:00Z"
  },
  {
    "temp": 9.434534534353345,
    "date": "2018-08-07T00:00:00Z"
  }
]
```

2: `GET /speeds?start=2018-08-01T00:00:00Z&end=2018-08-04T00:00:00Z`

This method will return each daily wind speed starting at the start date and ending at (but including) the end date.
The resulting wind speeds should be in the correct order
The parameter start should be an ISO8601 DateTime. The parameter end should be an ISO8601 DateTime.
The dates given are just examples. The method should work for any range of dates provided by the backing services. If any date is not available then the service should return an appropriate error with a JSON formatted error message

The response should be formatted as JSON and should look something like the following:
```json
[
  {
    "north": -17.989980201472466,
    "west": 16.300917971882726,
    "date": "2018-08-01T00:00:00Z"
  },
  {
    "north": 5.989980201472466,
    "west": 10.300917971882726,
    "date": "2018-08-02T00:00:00Z"
  },
  {
    "north": -20.989980201472466,
    "west": -16.300917971882726,
    "date": "2018-08-03T00:00:00Z"
  },
  {
    "north": 10.989980201472466,
    "west": -15.300917971882726,
    "date": "2018-08-04T00:00:00Z"
  }
]
```

3: `GET /weather?start=2018-08-01T00:00:00Z&end=2018-08-04T00:00:00Z`
This method will return each daily temperature and weather reading starting at the start date and ending at (but including) the end date.
The resulting weather reports should be in the correct order.
The parameter start should be an ISO8601 DateTime. The parameter end should be an ISO8601 DateTime.
The dates given are just examples. The method should work for any range of dates provided by the backing services. If any date is not available then the service should return an appropriate error with a JSON formatted error message

The response should be formatted as JSON and should look something like the following
```json
[
  {
    "north": -17.989980201472466,
    "west": 16.300917971882726,
    "temp": 10.46941232124016,
    "date": "2018-08-01T00:00:00Z"
  },
  {
    "north": 5.989980201472466,
    "west": 10.300917971882726,
    "temp": 13.5353456555445,
    "date": "2018-08-02T00:00:00Z"
  },
  {
    "north": -20.989980201472466,
    "west": -16.300917971882726,
    "temp": 8.23423423423344,
    "date": "2018-08-03T00:00:00Z"
  },
  {
    "north": 10.989980201472466,
    "west": -15.300917971882726,
    "temp": 11.6456546546454,
    "date": "2018-08-04T00:00:00Z"
  }
]
```

## HINT

* The windspeed service will be visible to your service (when running in docker compose) with the host name "windspeed". Similarly the temperature service will be visible to your service (when running in docker compose) with the host name "temperature". You'll still need to use the configured ports
* If you are developing your solution in something like Node.js or Go, think about how many concurrent requests you should be sending to the backing services at one time.


## Deliverables

* The source for the service described above
* A Dockerfile to run the service you have developed
* A Set of tests for the service you have developed, preferably in a second Dockerfile so we don't have to install whichever language you developed the solution and tests in
* A modified docker compose file which will start the backing services (already done) and your service implementation and (if you present them as a docker file) your tests

These should be uploaded to a public github/gitlab/bitbucket repository so that we can clone it and examine your solution.

## Assessment

The task itself is relatively simple, although if you are not familiar with docker or docker-compose you may have a little learning to do. We will be assessing you on your clean coding style, your correct error handling and your appropriate use of tests. This is a tech test though so while we would like you to do your best we do not expect your work to be perfect!