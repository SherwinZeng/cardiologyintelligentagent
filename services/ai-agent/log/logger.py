import logging
import sys
from datetime import datetime
from typing import Optional


class SpringBootFormatter(logging.Formatter):
    COLORS = {
        'DEBUG': '\033[36m',  # 青色
        'INFO': '\033[32m',  # 绿色
        'WARNING': '\033[33m',  # 黄色
        'ERROR': '\033[31m',  # 红色
        'CRITICAL': '\033[35m',  # 紫色
        'RESET': '\033[0m'
    }

    def __init__(self, use_color: bool = True, app_name: str = "zxr-cardiologyintelligentagent"):
        super().__init__()
        self.use_color = use_color and sys.stdout.isatty()
        self.app_name = app_name

    def format(self, record):
        now = datetime.fromtimestamp(record.created)
        time_str = now.strftime('%Y-%m-%d %H:%M:%S') + f".{int(record.msecs):03d}"
        levelname = record.levelname
        if self.use_color:
            levelname = f"{self.COLORS.get(levelname, '')}{levelname}{self.COLORS['RESET']}"
        thread_name = record.threadName[:10]
        message = super().format(record)
        log_line = f"{time_str} {levelname} [{self.app_name}] [{thread_name}] : {message}"
        return log_line


def get_logger(name: Optional[str] = None, level: str = "INFO") -> logging.Logger:
    if name is None:
        name = "zxr.cardiology.agent"

    logger = logging.getLogger(name)
    if logger.handlers:
        return logger

    logger.setLevel(getattr(logging, level.upper(), logging.INFO))
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setFormatter(SpringBootFormatter(app_name="zxr-cardiologyintelligentagent"))
    logger.addHandler(console_handler)
    return logger

def get_app_logger():
    return get_logger("zxr.cardiology.agent")