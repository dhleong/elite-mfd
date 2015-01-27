# elite-mfd [![Build Status](http://img.shields.io/travis/dhleong/elite-mfd.svg?style=flat)](https://travis-ci.org/dhleong/elite-mfd)

Fancy, contextual Elite: Dangerous tools

## Features

Elite MFD tracks your current system by enabling Verbose Logging. This makes it more convenient to use various popular E:D tools:

- Trade calculation relative to current system/station
- Search for nearby stations matching various criteria
- Navigation/routing with turn-by-turn narration
- Automatic memorization of various search parameters like ship size, jump range, and cash available

The MFD also provides other convenience tools:

- Macro system, with a default for requesting landing permission
- Login and logout with the press of a button

## Installation

For now, clone the repo somewhere, then install the UI stuff using

    cd resources/public && bower install

Pre-built standalone jars may be provided in [Releases](https://github.com/dhleong/elite-mfd/releases) at some point.

## Usage

For development, just use [leiningen](http://leiningen.org/):

    lein run

For faster startup, you can build a static jar using:

    lein uberjar

Static jars can be launched with [Launch4J](http://launch4j.sourceforge.net/) in Windows, if that's what you're into.

Once the server is running, open a web browser from your preferred device pointing to the computer's IP and port `9876`, eg: if your computer has the LAN IP address `192.168.1.6`, open `http://192.168.1.6:9876`. 

## Credits

- Trading data is taken from [Thrudd's Trading Tools](http://www.elitetradingtool.co.uk/)
- Navigation routes from [CMDR Club](http://cmdr.club)
- Narration audio created using [Google Translate](http://translate.google.com)
- Elite: Dangerous and all associated media are the intellectual property of [Frontier Developments](http://www.frontier.co.uk)

## Disclaimer

The software is provided as-is. No warantee or guarantee of any kind is provided whatsoever. The author takes NO responsibility for any harm you may cause or incur as a result of using this software. Use at your own risk, etc.

Be sure to follow normal best practices for network security (IE: don't leave all your ports open to the universe, etc). Elite MFD

## License

Copyright © 2015 Daniel Leong

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
