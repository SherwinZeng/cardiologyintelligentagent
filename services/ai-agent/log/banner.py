import sys
from datetime import datetime
from django.conf import settings

BANNER = r"""
   ___  _  _  ____   ____   _  _   ___   _  _   ___   ____   _  _   ___   ____   _  _   ___   ____ 
  / _ \| || ||_  _| |_  _| | || | |_ _| | \| | |_ _| |_  _| | || | |_ _| |_  _| | || | |_ _| |_  _|
 | (_) | || |  ||     ||   | __ |  | |  | .` |  | |    ||   | __ |  | |    ||   | __ |  | |    ||  
  \__\_\____| |_|    |_|   |_||_| |___| |_|\_| |___|   |_|   |_||_| |___|   |_|   |_||_| |___|   |_|  

"""


def print_banner():
    print(BANNER)

    project_name = "zxr-cardiologyintelligentagent"
    version = "0.1.0"
    print(f":: {project_name} :: (v{version})")
    print()

    current_time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"Started ZXRCardiologyAgent in {get_startup_time():.2f} seconds (process running)")
    print(f"Active profile: {'development' if settings.DEBUG else 'production'}")
    print(f"Server started at http://127.0.0.1:8000")
    print()


_start_time = None

def set_start_time():
    global _start_time
    _start_time = datetime.now()

def get_startup_time() -> float:
    if _start_time:
        return (datetime.now() - _start_time).total_seconds()
    return 0.0