# macOS Control Center Installation

textus-launcher will use cncf-launcher's generic platform-Subcomponent support to install the Textus Control Center macOS Menu Bar application.

Initial flow: obtain textus-control-center CAR, resolve the macOS/architecture Subcomponent, extract the .app, place it in the user application area, register/start it as a login item, and connect it to the local Control Center. The exact filesystem/login-item implementation remains an implementation detail.

The same flow must work whether the Menu Bar Subcomponent is bundled in the main CAR or supplied by a separate CAR.
