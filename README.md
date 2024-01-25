[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/W7W4KMMB4)
<br/>
<p align="center">
  <h3 align="center">ForcePack</h3>

  <p align="center">
    Minecraft plugin to force players to use your server resource pack, among other utilities.
    <br/>
    <br/>
    <a href="https://github.com/SamB440/ForcePack/issues">Report Bug / Feature request</a>
  </p>

  <center>
    <a href="https://discord.gg/fh62mxU">
      <img alt="Discord" src="https://img.shields.io/discord/282242806695591938">
    </a>
  </center>
</p>

[![CodeFactor](https://www.codefactor.io/repository/github/samb440/forcepack/badge/master)](https://www.codefactor.io/repository/github/samb440/forcepack/overview/master) ![Contributors](https://img.shields.io/github/contributors/SamB440/ForcePack?color=dark-green) ![Issues](https://img.shields.io/github/issues/SamB440/ForcePack) ![License](https://img.shields.io/github/license/SamB440/ForcePack)
![Forks](https://img.shields.io/github/forks/SamB440/ForcePack?style=social) ![Stargazers](https://img.shields.io/github/stars/SamB440/ForcePack?style=social)

## Table Of Contents

* [About the Project](#about-the-project)
* [Features](#features)
* [Getting Started](#getting-started)
    * [Prerequisites](#prerequisites)
    * [Setting up](#setting-up)
* [What to do](#what-to-do)
* [Contributing](#contributing)
* [License](#license)

## About The Project

ForcePack facilitates forcing users to accept your resource pack, with utilities such as live reloading, SHA-1 generation, and per-server support via velocity.

You can find the Spigot resource at: https://www.spigotmc.org/resources/forcepack.45439/

## Features
- Support for 1.20.3+ multiple resource packs
- Ability to set resource packs on a per-version basis
- Local webserver resource pack hosting
- Pack unloading for Velocity sub-servers without a resource pack
- Prevents escaping out bypass for clients <= 1.12
- Run commands on pack statuses
- RGB via MiniMessage support
- Prevent failed downloads from bypassing the resource pack
- Geyser support to ignore bedrock players
- Per-client version maximum size verification
- SHA-1 and URL pack verification
- SHA-1 Auto-generation
- Per-server support on Velocity
- Live reloading with resource pack updating
- Easy to use custom force resource pack message for 1.17+ clients

## Getting Started

To contribute, follow the steps below. Contributions are very welcome.

### Prerequisites

* [JDK 17](https://adoptium.net/)
* [Gradle](https://gradle.org/)
* [Git](https://gitforwindows.org/), if on windows.

### Setting up

Clone the repository
Via IntelliJ:
```File > New > Project from Version Control > URL: https://github.com/SamB440/ForcePack.git > Clone```

Or, via git bash:
```sh
git clone https://github.com/SamB440/ForcePack.git
```

To build, run the `build` task from IntelliJ, gradle, or gradlew.

## What to do

See the [open issues](https://github.com/SamB440/ForcePack/issues) for a list of proposed features (and known issues).

## Contributing

Contributions are greatly welcomed.
* If you have suggestions for new features or changes, feel free to [open an issue](https://github.com/SamB440/ForcePack/issues/new) to discuss it, or directly create a pull request.
* Make sure to comment your code and add javadoc headers!
* Create an individual PR for each suggestion.

### Creating A Pull Request

Please be mindful that we may ask you to make changes to your pull requests.

Also, if possible, please use `feature` or `fix` branch prefixes.

## License

Distributed under the GNU GPL v3 License. See [LICENSE](https://github.com/SamB440/ForcePack/blob/master/LICENSE) for more information.
