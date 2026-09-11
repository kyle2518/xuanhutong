from .config import get_settings
from .database import MetadataDatabase
from .services.rag_service import RagService
from .services.vector_store import VectorStoreProvider

settings = get_settings()
db = MetadataDatabase(settings.metadata_db_path)
store = VectorStoreProvider(settings)
rag = RagService(settings, store)