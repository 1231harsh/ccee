const SUBJECTS = [
  {
    code: "JAVA",
    title: "Java Programming",
    summary: "Core syntax, OOP, collections, multithreading, JDBC, and file handling.",
  },
  {
    code: "DBMS",
    title: "Database Management Systems",
    summary: "Modeling, SQL, joins, transactions, normalization, and query optimization.",
  },
  {
    code: "CPP",
    title: "C++ Programming",
    summary: "OOP basics, templates, STL, exceptions, polymorphism, and memory handling.",
  },
  {
    code: "WEB",
    title: "Web Technologies",
    summary: "Servlets, JSP, MVC, sessions, JDBC integration, security, and REST APIs.",
  },
];

function Home({ loading, onStartTest, onViewAnalysis }) {
  return (
    <main className="dashboard">
      <section className="hero-panel">
        <div>
          <p className="eyebrow">Practice Dashboard</p>
          <h2>Choose a subject and launch a secure mock test.</h2>
          <p className="hero-copy">
            Each test session is issued by the backend, scored server-side, and
            tied to your authenticated account for analysis.
          </p>
        </div>
        <button className="button button-secondary" onClick={onViewAnalysis}>
          Open Performance Analysis
        </button>
      </section>

      <section className="subject-grid">
        {SUBJECTS.map((subject) => (
          <article className="panel-card subject-card" key={subject.code}>
            <p className="subject-code">{subject.code}</p>
            <h3>{subject.title}</h3>
            <p>{subject.summary}</p>
            <button
              className="button button-primary"
              disabled={loading}
              onClick={() => onStartTest(subject.code)}
            >
              {loading ? "Preparing..." : `Start ${subject.code} Test`}
            </button>
          </article>
        ))}
      </section>
    </main>
  );
}

export default Home;
