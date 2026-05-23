"""Mock Firebase runtime dependencies that aren't pip-installable."""

import sys
from unittest.mock import MagicMock

# firebase_functions is only available in the Cloud Functions runtime.
# Mock it so we can import main.py in tests.
firebase_functions = MagicMock()


class _MockHttpsError(Exception):
    def __init__(self, code=None, message=""):
        self.code = code
        super().__init__(message)


firebase_functions.https_fn.HttpsError = _MockHttpsError
firebase_functions.https_fn.FunctionsErrorCode = MagicMock()
firebase_functions.https_fn.on_call = lambda **kwargs: lambda fn: fn
firebase_functions.options.MemoryOption.MB_256 = "256MiB"

sys.modules["firebase_functions"] = firebase_functions
sys.modules["firebase_functions.https_fn"] = firebase_functions.https_fn
sys.modules["firebase_functions.options"] = firebase_functions.options

# firebase_admin — mock only the parts main.py uses at import time
firebase_admin = MagicMock()
sys.modules["firebase_admin"] = firebase_admin
sys.modules["firebase_admin.firestore"] = firebase_admin.firestore
sys.modules["firebase_admin.messaging"] = firebase_admin.messaging
