# Mobiusbobs Android Video Processing Library

## TODO

- testing platform for
  - limitation of total number of MediaCodec instance

- refactor ideas
  - abstract video and audio decode/encode to reduce duplicated code
    - encode
    - decode
    - decode -> process -> encode
  - presentation time calculator
  - extract mideacodec helper
  - extract video processing builder
  - rebuild the processing system bottom up
  - add logs
  - processing pipeline

## Check list of file to refactor

- codec.Extractor

## Feature Requests

- configurable video size
