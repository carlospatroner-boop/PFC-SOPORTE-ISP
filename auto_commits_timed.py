import subprocess
import time
import random
from datetime import datetime

commits = [
    {"num": 1, "author": "Carlos Carpio", "email": "carlospatroner@gmail.com", "files": ["db-cluster/scripts/init_db.sql", "db-cluster/config/zones.sql", "db-cluster/docker-compose.cockroach.yml", "db-cluster/scripts/queries_bench.sql", "db-cluster/scripts/seed_partitioned.sql"], "msg": "feat(db-cluster): reparticionar tickets por fecha_apertura y ajustar memoria de los nodos"},
    {"num": 2, "author": "Cristhian Pacheco", "email": "cristhianpachecoc03@gmail.com", "files": ["services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/domain/Ticket.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/domain/TicketId.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/repository/TicketRepository.java"], "msg": "refactor(ticket-service): adaptar entidad Ticket y repositorio a la clave (created_at, id)"},
    {"num": 3, "author": "Robinson Cando", "email": "rcandom@uteq.edu.ec", "files": ["services/svc-principal/src/test/java/ec/edu/uteq/soporte/ticketservice/service/TicketServiceTest.java", "services/svc-principal/src/test/java/ec/edu/uteq/soporte/ticketservice/service/TicketClassificationListenerTest.java"], "msg": "test(ticket-service): actualizar pruebas para la nueva clave y el reintento serializable"},
    {"num": 4, "author": "Jeremy Álvarez", "email": "jalvarezp3@uteq.edu.ec", "files": ["docs/adr/0003-sharding-policy.md", "docs/diagrams/"], "msg": "docs(architecture): actualizar ADR de fragmentacion y diagramas C4"},
    {"num": 5, "author": "Cristhian Pacheco", "email": "cristhianpachecoc03@gmail.com", "files": ["services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/service/TicketService.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/service/TicketNotFoundException.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/service/TicketClassificationListener.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/web/TicketController.java"], "msg": "refactor(ticket-service): simplificar API a /tickets/{id} sin la zona en la ruta"},
    {"num": 6, "author": "Robinson Cando", "email": "rcandom@uteq.edu.ec", "files": ["services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/config/CrdbMetrics.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/config/RepositoryTimingAspect.java", "services/svc-principal/src/test/java/ec/edu/uteq/soporte/ticketservice/config/CrdbMetricsTest.java", "services/svc-principal/pom.xml", "services/svc-principal/src/main/resources/application.yml"], "msg": "feat(ticket-service): instrumentar metricas Prometheus (crdb_query_duration_seconds, crdb_transaction_retries_total, crdb_pool_active_connections)"},
    {"num": 7, "author": "Jeremy Álvarez", "email": "jalvarezp3@uteq.edu.ec", "files": ["services/notification-service/"], "msg": "feat(notification-service): agregar microservicio de notificaciones multicanal"},
    {"num": 8, "author": "Cristhian Pacheco", "email": "cristhianpachecoc03@gmail.com", "files": ["services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/event/TicketCreatedEvent.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/event/TicketStatusChangedEvent.java", "services/svc-principal/src/main/java/ec/edu/uteq/soporte/ticketservice/event/TicketAssignedEvent.java"], "msg": "feat(ticket-service): enriquecer ticket.created y agregar eventos status-changed/assigned"},
    {"num": 9, "author": "Robinson Cando", "email": "rcandom@uteq.edu.ec", "files": ["services/svc-principal/src/test/java/ec/edu/uteq/soporte/ticketservice/integration/TicketRepositoryIntegrationTest.java"], "msg": "test(ticket-service): agregar pruebas de integracion con Testcontainers contra CockroachDB real"},
    {"num": 10, "author": "Jeremy Álvarez", "email": "jalvarezp3@uteq.edu.ec", "files": ["services/ai-service/"], "msg": "feat(ai-service): agregar microservicio de clasificacion asincrona de tickets"},
    {"num": 11, "author": "Carlos Carpio", "email": "carlospatroner@gmail.com", "files": ["services/report-service/", "db-cluster/scripts/init_report_db.sql"], "msg": "feat(report-service): agregar microservicio de reportes con modelo de lectura CQRS"},
    {"num": 12, "author": "Carlos Carpio", "email": "carlospatroner@gmail.com", "files": ["frontend/"], "msg": "feat(frontend): agregar dashboard de tickets, panel admin y vista de reportes"},
    {"num": 13, "author": "Cristhian Pacheco", "email": "cristhianpachecoc03@gmail.com", "files": ["spark/src/generate_dataset.py", "spark/src/transformations.py", "spark/src/pipeline.py", "spark/Dockerfile", "spark/requirements.txt"], "msg": "feat(spark): redisenar el pipeline con las 5 transformaciones exigidas (filtrado, join, ventana, tipos temporales, clustering ML)"},
    {"num": 14, "author": "Robinson Cando", "email": "rcandom@uteq.edu.ec", "files": ["spark/src/baseline.py", "spark/src/protocol_experiment.py", "spark/notebooks/", "spark/README.md", "docs/experimentos/"], "msg": "feat(spark): agregar baseline pandas y protocolo experimental JCR (r=10, IC95%)"},
    {"num": 15, "author": "Carlos Carpio", "email": "carlospatroner@gmail.com", "files": ["start-all.ps1"], "msg": "chore(devops): agregar script de arranque unico para los 5 microservicios"},
    {"num": 16, "author": "Jeremy Álvarez", "email": "jalvarezp3@uteq.edu.ec", "files": ["README.md", "LICENSE", ".env.example", "ai-usage-declaration.md", ".github/workflows/ci.yml", "docs/api/"], "msg": "docs(repo): agregar README raiz, LICENSE, .env.example, declaracion de uso de IA y CI"}
]

total = len(commits)

# Cambiar a la rama objetivo
subprocess.run(["git", "checkout", "feature/entrega-3"], check=True)

for idx, c in enumerate(commits):
    now_str = datetime.now().strftime("%H:%M:%S")
    print("\n==================================================")
    print(f"[{now_str}] Ejecutando Commit {c['num']}/{total}: {c['author']}")
    print(f"Mensaje: {c['msg']}")
    print("==================================================")
    
    # Configurar la identidad del autor
    subprocess.run(["git", "config", "user.name", c["author"]], check=True)
    subprocess.run(["git", "config", "user.email", c["email"]], check=True)
    
    # Agregar archivos
    for f in c["files"]:
        subprocess.run(["git", "add", f], check=True)
        
    # Crear commit
    subprocess.run(["git", "commit", "-m", c["msg"]], check=True)
    
    # Push a la rama remota feature/entrega-3
    push_cmd = ["git", "push", "-u", "origin", "feature/entrega-3"] if c["num"] == 1 else ["git", "push"]
    subprocess.run(push_cmd, check=True)
    
    # Pausa de 15 a 20 minutos entre commits (salvo en el último)
    if idx < total - 1:
        wait_seconds = random.randint(15 * 60, 20 * 60)
        minutes = wait_seconds // 60
        seconds = wait_seconds % 60
        print(f"\n⏳ Esperando {minutes}m {seconds}s antes del siguiente commit...")
        time.sleep(wait_seconds)

print("\n🎉 ¡Proceso completado! Los 16 commits fueron subidos exitosamente a feature/entrega-3.")