package textus.launcher

/*
 * @since   May. 21, 2026
 * @version May. 21, 2026
 * @author  ASAMI, Tomoharu
 */
final case class RuntimeRequirement(
  minimum: Option[String] = None,
  maximum: Option[String] = None,
  excluded: Vector[String] = Vector.empty,
  tested: Vector[String] = Vector.empty,
  source: String = "unknown"
) {
  def isEmpty: Boolean =
    minimum.isEmpty && maximum.isEmpty && excluded.isEmpty && tested.isEmpty

  def accepts(version: String): Boolean =
    !excluded.contains(version) &&
      minimum.forall(RuntimeVersionOrdering.compare(version, _) >= 0) &&
      maximum.forall(RuntimeVersionOrdering.compare(version, _) <= 0)

  def testedContains(version: String): Boolean =
    tested.contains(version)

  def withSource(value: String): RuntimeRequirement =
    copy(source = value)
}

enum RuntimeNoCompatiblePolicy {
  case Error, Newest
}

object RuntimeNoCompatiblePolicy {
  def parse(value: String): RuntimeNoCompatiblePolicy =
    value match {
      case "error" => Error
      case "newest" => Newest
      case other => throw TextusException(s"unknown runtime no-compatible policy: $other")
    }

  def render(policy: RuntimeNoCompatiblePolicy): String =
    policy match {
      case Error => "error"
      case Newest => "newest"
    }
}

enum RuntimeSelectionPolicy {
  case CurrentCompatible, TestedLatest, LatestCompatible, NewestCompatible
}

object RuntimeSelectionPolicy {
  def parse(value: String): RuntimeSelectionPolicy =
    value match {
      case "current" | "current-compatible" => CurrentCompatible
      case "tested" | "tested-latest" => TestedLatest
      case "latest" | "latest-compatible" | "compatible" | "compatible-latest" => LatestCompatible
      case "newest" | "newest-compatible" => NewestCompatible
      case other => throw TextusException(s"unknown runtime selection policy: $other")
    }

  def render(policy: RuntimeSelectionPolicy): String =
    policy match {
      case CurrentCompatible => "current-compatible"
      case TestedLatest => "tested-latest"
      case LatestCompatible => "latest-compatible"
      case NewestCompatible => "newest-compatible"
    }
}

object RuntimeVersionSelection {
  def select(
    requested: Option[String],
    stored: String,
    requirements: Vector[RuntimeRequirement],
    catalog: Option[RuntimeCatalog],
    selectionPolicy: RuntimeSelectionPolicy,
    policy: RuntimeNoCompatiblePolicy
  ): String = {
    val effective = requirements.filterNot(_.isEmpty)
    requested match {
      case Some(selector) =>
        val version = _resolve_selector(selector, catalog)
        _validate_requested(version, effective)
        version
      case None if effective.nonEmpty =>
        val runtimecatalog = catalog.getOrElse(throw TextusException("component-driven runtime selection requires CNCF runtime catalog"))
        _select_from_catalog(stored, runtimecatalog, effective, selectionPolicy, policy)
      case None =>
        stored
    }
  }

  private def _resolve_selector(
    selector: String,
    catalog: Option[RuntimeCatalog]
  ): String =
    catalog.map(_.resolve(selector).version).getOrElse(selector)

  private def _validate_requested(
    version: String,
    requirements: Vector[RuntimeRequirement]
  ): Unit = {
    val rejected = requirements.filterNot(_.accepts(version))
    if (rejected.nonEmpty) {
      val sources = rejected.map(_.source).distinct.sorted.mkString(", ")
      throw TextusException(s"CNCF runtime $version is not compatible with component requirements: $sources")
    }
  }

  private def _select_from_catalog(
    stored: String,
    catalog: RuntimeCatalog,
    requirements: Vector[RuntimeRequirement],
    selectionpolicy: RuntimeSelectionPolicy,
    policy: RuntimeNoCompatiblePolicy
  ): String = {
    val compatible = catalog.enabledVersions.filter(v => requirements.forall(_.accepts(v.version)))
    if (compatible.isEmpty) {
      policy match {
        case RuntimeNoCompatiblePolicy.Error =>
          val sources = requirements.map(_.source).distinct.sorted.mkString(", ")
          throw TextusException(s"no compatible CNCF runtime version for component requirements: $sources")
        case RuntimeNoCompatiblePolicy.Newest =>
          val newest = catalog.newest.version
          Console.err.println(s"warning: no compatible CNCF runtime version; using newest CNCF runtime $newest")
          newest
      }
    } else {
      selectionpolicy match {
        case RuntimeSelectionPolicy.CurrentCompatible =>
          val current = _resolve_selector(stored, Some(catalog))
          if (requirements.forall(_.accepts(current))) current else _latest(catalog, compatible).version
        case RuntimeSelectionPolicy.TestedLatest =>
          val withtested = compatible.filter(v => requirements.forall(r => r.tested.isEmpty || r.testedContains(v.version)))
          _latest(catalog, if (withtested.nonEmpty) withtested else compatible).version
        case RuntimeSelectionPolicy.LatestCompatible =>
          _latest(catalog, compatible).version
        case RuntimeSelectionPolicy.NewestCompatible =>
          _newest(compatible).version
      }
    }
  }

  private def _newest(versions: Vector[RuntimeCatalogVersion]): RuntimeCatalogVersion =
    versions.sortBy(v => (v.publishedAt.getOrElse(""), v.version)).last

  private def _latest(
    catalog: RuntimeCatalog,
    versions: Vector[RuntimeCatalogVersion]
  ): RuntimeCatalogVersion =
    catalog.latestStable.flatMap(version => versions.find(_.version == version)).getOrElse {
      _latest_candidate(versions)
    }

  private def _latest_candidate(versions: Vector[RuntimeCatalogVersion]): RuntimeCatalogVersion = {
    val stable = versions.filter(v => !v.channel.contains("snapshot") && !v.version.toUpperCase.contains("SNAPSHOT"))
    _newest(if (stable.nonEmpty) stable else versions)
  }
}

object RuntimeVersionOrdering {
  def compare(a: String, b: String): Int = {
    val ax = _tokens(a)
    val bx = _tokens(b)
    val max = math.max(ax.length, bx.length)
    var i = 0
    while (i < max) {
      val av = ax.lift(i).getOrElse("0")
      val bv = bx.lift(i).getOrElse("0")
      val c = _compare_token(av, bv)
      if (c != 0)
        return c
      i += 1
    }
    0
  }

  private def _tokens(value: String): Vector[String] =
    value.split("[^A-Za-z0-9]+").toVector.filter(_.nonEmpty)

  private def _compare_token(a: String, b: String): Int = {
    val an = a.forall(_.isDigit)
    val bn = b.forall(_.isDigit)
    if (an && bn)
      BigInt(a).compare(BigInt(b))
    else if (a.equalsIgnoreCase("SNAPSHOT") && !b.equalsIgnoreCase("SNAPSHOT"))
      -1
    else if (!a.equalsIgnoreCase("SNAPSHOT") && b.equalsIgnoreCase("SNAPSHOT"))
      1
    else
      a.compareToIgnoreCase(b)
  }
}
