import {
  Suspense,
  useEffect,
  useMemo,
  useRef,
  useState,
  type RefObject,
} from "react";
import { Canvas, useFrame, useThree } from "@react-three/fiber";
import {
  ContactShadows,
  Edges,
  Html,
  Line,
  OrbitControls,
  PerformanceMonitor,
} from "@react-three/drei";
import {
  Bloom,
  EffectComposer,
  N8AO,
  ToneMapping,
  Vignette,
} from "@react-three/postprocessing";
import { BlendFunction, ToneMappingMode } from "postprocessing";
import * as THREE from "three";
import gsap from "gsap";
import {
  positionForStage,
  type SimNode,
  type SimStage,
  BUDGET_CAP,
} from "../../data/portfolio";
import { useTheme, type Theme } from "../../theme/ThemeProvider";
import { cn } from "../../lib/cn";

/* Status hues — Anthropic clay / olive / amber */
const COL_IN = "#d97757";
const COL_OUT = "#a73d39";
const COL_WARN = "#b58a3c";
const COL_LINK = "#87867f";

const COL_IN_C = new THREE.Color(COL_IN);
const COL_BLACK = new THREE.Color("#000000");

/** Shared unit box — towers scale it instead of allocating unique geometries. */
const UNIT_BOX = new THREE.BoxGeometry(1, 1, 1);
const RAIL_BOX = new THREE.BoxGeometry(1, 1, 1);
const PEDESTAL_BOX = new THREE.BoxGeometry(1, 1, 1);
const FLOOR_GEO = new THREE.CircleGeometry(18, 32);

type Quality = "high" | "medium" | "low";

function scenePalette(theme: Theme) {
  if (theme === "light") {
    return {
      bg: "#faf9f5",
      idle: "#5e5d59",
      hot: "#141413",
      norm: "#73726c",
      floor: "#f0eee6",
      grid: "#d1cfc5",
      gridDim: "#e8e6dc",
      dust: "#87867f",
      pedestal: "#dedcd1",
      ambient: 0.55,
      hemiSky: "#faf9f5",
      hemiGround: "#d1cfc5",
      hemi: 0.55,
      key: 1.15,
      fill: 0.28,
      rim: "#c2c0b6",
      aoColor: "#3d3d3a",
      bloom: 0.28,
      vignette: 0.32,
    };
  }
  return {
    bg: "#141413",
    idle: "#87867f",
    hot: "#faf9f5",
    norm: "#b0aea5",
    floor: "#1f1e1d",
    grid: "#30302e",
    gridDim: "#262624",
    dust: "#9c9a92",
    pedestal: "#262624",
    ambient: 0.28,
    hemiSky: "#30302e",
    hemiGround: "#141413",
    hemi: 0.42,
    key: 1.25,
    fill: 0.22,
    rim: "#5e5d59",
    aoColor: "#0a0a09",
    bloom: 0.42,
    vignette: 0.5,
  };
}

type Props = {
  nodes: SimNode[];
  stage: SimStage;
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  focusToken?: number;
  className?: string;
  /** Decorative / login background — lighter GPU budget. */
  lite?: boolean;
};

function usePrefersReducedMotion() {
  const [reduce, setReduce] = useState(false);
  useEffect(() => {
    const mq = window.matchMedia("(prefers-reduced-motion: reduce)");
    const sync = () => setReduce(mq.matches);
    sync();
    mq.addEventListener("change", sync);
    return () => mq.removeEventListener("change", sync);
  }, []);
  return reduce;
}

function useCanvasVisible(ref: RefObject<HTMLElement | null>) {
  const [visible, setVisible] = useState(true);
  useEffect(() => {
    const el = ref.current;
    if (!el || typeof IntersectionObserver === "undefined") return;
    const io = new IntersectionObserver(
      ([entry]) => setVisible(entry.isIntersecting && entry.intersectionRatio > 0.04),
      { threshold: [0, 0.04, 0.12] },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [ref]);
  return visible;
}

function Floor({ theme, quality }: { theme: Theme; quality: Quality }) {
  const palette = scenePalette(theme);
  const grid = useMemo(() => {
    const g = new THREE.GridHelper(36, quality === "low" ? 18 : 28, palette.grid, palette.gridDim);
    const mat = g.material as THREE.Material | THREE.Material[];
    const apply = (m: THREE.Material) => {
      m.transparent = true;
      m.opacity = theme === "light" ? 0.55 : 0.4;
    };
    if (Array.isArray(mat)) mat.forEach(apply);
    else apply(mat);
    return g;
  }, [palette.grid, palette.gridDim, theme, quality]);

  return (
    <>
      <primitive object={grid} position={[0, -0.01, 0]} />
      <mesh
        rotation={[-Math.PI / 2, 0, 0]}
        position={[0, -0.025, 0]}
        geometry={FLOOR_GEO}
        receiveShadow={quality !== "low"}
      >
        <meshStandardMaterial
          color={palette.floor}
          metalness={theme === "light" ? 0.08 : 0.28}
          roughness={theme === "light" ? 0.94 : 0.9}
          transparent
          opacity={0.97}
        />
      </mesh>
      {quality === "high" && (
        <ContactShadows
          position={[0, 0.01, 0]}
          opacity={theme === "light" ? 0.22 : 0.36}
          scale={28}
          blur={1.8}
          far={6}
          resolution={256}
          frames={1}
          color={theme === "light" ? "#3d3d3a" : "#000000"}
        />
      )}
    </>
  );
}

function Dust({ count, theme, reduce }: { count: number; theme: Theme; reduce: boolean }) {
  const ref = useRef<THREE.Points>(null);
  const palette = scenePalette(theme);
  const positions = useMemo(() => {
    const arr = new Float32Array(count * 3);
    let seed = 41;
    const next = () => {
      seed = (seed * 16807) % 2147483647;
      return (seed - 1) / 2147483646;
    };
    for (let i = 0; i < count; i++) {
      arr[i * 3] = (next() - 0.5) * 34;
      arr[i * 3 + 1] = next() * 9;
      arr[i * 3 + 2] = (next() - 0.5) * 34;
    }
    return arr;
  }, [count]);

  useFrame((_, dt) => {
    if (!ref.current || reduce) return;
    ref.current.rotation.y += dt * 0.01;
    ref.current.position.y = Math.sin(performance.now() * 0.00015) * 0.08;
  });

  return (
    <points ref={ref} frustumCulled>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
      </bufferGeometry>
      <pointsMaterial
        size={0.018}
        color={palette.dust}
        transparent
        opacity={theme === "light" ? 0.14 : 0.2}
        depthWrite={false}
        sizeAttenuation
      />
    </points>
  );
}

/**
 * Dependency energy link — dashed Line with animated dashOffset.
 * Communicates graph flow during the dependencies stage / when highlighted.
 */
function FlowLink({
  a,
  b,
  color,
  opacity,
  lineWidth,
  flowing,
  reduce,
}: {
  a: THREE.Vector3;
  b: THREE.Vector3;
  color: string;
  opacity: number;
  lineWidth: number;
  flowing: boolean;
  reduce: boolean;
}) {
  const mid = useMemo(() => {
    const m = a.clone().lerp(b, 0.5);
    m.y += 0.35;
    return m;
  }, [a, b]);

  const points = useMemo(
    () => [
      [a.x, a.y, a.z] as [number, number, number],
      [mid.x, mid.y, mid.z] as [number, number, number],
      [b.x, b.y, b.z] as [number, number, number],
    ],
    [a, b, mid],
  );

  const lineRef = useRef<THREE.Group>(null);

  useFrame((_, dt) => {
    if (!flowing || reduce || !lineRef.current) return;
    const line = lineRef.current.children[0] as THREE.Object3D & {
      material?: THREE.Material & { dashOffset?: number };
    };
    const mat = line?.material;
    if (mat && typeof mat.dashOffset === "number") {
      mat.dashOffset -= dt * 1.6;
    }
  });

  return (
    <group ref={lineRef}>
      <Line
        points={points}
        color={color}
        lineWidth={lineWidth}
        transparent
        opacity={opacity}
        dashed={flowing}
        dashSize={flowing ? 0.28 : 1}
        gapSize={flowing ? 0.16 : 0}
        dashScale={1}
      />
    </group>
  );
}

function ProjectTower({
  node,
  stage,
  index,
  total,
  selectedIndex,
  selectedCount,
  maxNpv,
  active,
  depHighlight,
  theme,
  reduce,
  quality,
  onSelect,
  onHover,
}: {
  node: SimNode;
  stage: SimStage;
  index: number;
  total: number;
  selectedIndex: number;
  selectedCount: number;
  maxNpv: number;
  active: boolean;
  depHighlight: boolean;
  theme: Theme;
  reduce: boolean;
  quality: Quality;
  onSelect: (id: string) => void;
  onHover: (id: string | null) => void;
}) {
  const palette = scenePalette(theme);
  const group = useRef<THREE.Group>(null);
  const body = useRef<THREE.Mesh>(null);
  const rail = useRef<THREE.Mesh>(null);
  const pedestal = useRef<THREE.Mesh>(null);
  const initialPos = positionForStage(node, stage, index, total, selectedIndex, selectedCount, maxNpv);
  const target = useRef(new THREE.Vector3(...initialPos));
  const current = useRef(new THREE.Vector3(...initialPos));
  const [hovered, setHovered] = useState(false);
  const pulse = useRef(0);

  const maxBudget = BUDGET_CAP;
  const height = 0.5 + (node.budget / maxBudget) * 5.2;
  const width = 0.26 + (node.budget / maxBudget) * 0.4;
  const depth = width * 0.72;
  const showEdges = quality === "high" && (active || hovered || depHighlight);

  useEffect(() => {
    target.current.set(
      ...positionForStage(node, stage, index, total, selectedIndex, selectedCount, maxNpv),
    );
  }, [node, stage, index, total, selectedIndex, selectedCount, maxNpv]);

  const colorHex = useMemo(() => {
    if (!node.evaluated && stage === "normalize") return palette.norm;
    if (stage === "knapsack" && node.evaluated && !node.selected) return COL_WARN;
    if (stage === "dependencies") return node.selected ? COL_IN : COL_OUT;
    if (stage === "final" || stage === "done") return node.selected ? COL_IN : COL_OUT;
    if (active || hovered || depHighlight) return palette.hot;
    return palette.idle;
  }, [stage, node, active, hovered, depHighlight, palette]);

  const targetColor = useMemo(() => new THREE.Color(colorHex), [colorHex]);

  const opacity =
    stage === "final" || stage === "done"
      ? node.selected
        ? 1
        : 0.16
      : node.evaluated || stage === "idle"
        ? 1
        : 0.22;

  const selectedGlow =
    node.selected && (stage === "final" || stage === "done" || stage === "dependencies");

  useFrame((state, dt) => {
    if (!group.current || !body.current) return;
    const follow = 1 - Math.exp(-2.6 * dt);
    current.current.lerp(target.current, follow);
    group.current.position.set(current.current.x, 0, current.current.z);

    if (!reduce && quality !== "low") {
      const dist = current.current.distanceTo(target.current);
      const settle = Math.max(0, 1 - dist * 2.2);
      pulse.current = THREE.MathUtils.lerp(pulse.current, settle * 0.04, 0.08);
      group.current.position.y = Math.sin(state.clock.elapsedTime * 2.2 + index) * pulse.current;
    }

    const lean =
      !node.selected && (stage === "final" || stage === "done")
        ? 0.42
        : active || hovered
          ? -0.04
          : 0;
    group.current.rotation.z = THREE.MathUtils.lerp(
      group.current.rotation.z,
      lean,
      1 - Math.exp(-3.2 * dt),
    );

    body.current.scale.set(width, height, depth);
    body.current.position.y = height / 2;

    const mat = body.current.material as THREE.MeshStandardMaterial;
    mat.color.lerp(targetColor, 1 - Math.exp(-5 * dt));

    const emissiveTarget = selectedGlow || active ? COL_IN_C : COL_BLACK;
    mat.emissive.lerp(emissiveTarget, 1 - Math.exp(-4 * dt));
    const glowIntensity = active
      ? 0.7
      : selectedGlow
        ? reduce
          ? 0.45
          : 0.48 + Math.sin(state.clock.elapsedTime * 2.4) * 0.06
        : depHighlight
          ? 0.22
          : 0.03;
    mat.emissiveIntensity = THREE.MathUtils.lerp(mat.emissiveIntensity, glowIntensity, 0.12);
    mat.opacity = THREE.MathUtils.lerp(mat.opacity, opacity, 1 - Math.exp(-4 * dt));
    mat.roughness = THREE.MathUtils.lerp(
      mat.roughness,
      selectedGlow || active ? 0.28 : 0.45,
      0.08,
    );

    if (rail.current) {
      const railH = Math.max(0.08, height * node.score);
      rail.current.scale.set(0.04, railH, 0.04);
      rail.current.position.set(width / 2 + 0.05, railH / 2, 0);
      const railMat = rail.current.material as THREE.MeshBasicMaterial;
      railMat.opacity = 0.75 * opacity;
    }
    if (pedestal.current) {
      pedestal.current.scale.set(width * 1.55, 0.05, depth * 1.45);
      pedestal.current.position.set(0, 0.025, 0);
      const pMat = pedestal.current.material as THREE.MeshStandardMaterial;
      pMat.opacity = opacity;
    }
  });

  const edge = active || hovered || depHighlight ? colorHex : COL_LINK;

  return (
    <group ref={group}>
      <mesh
        ref={body}
        geometry={UNIT_BOX}
        castShadow={quality === "high"}
        receiveShadow={false}
        onClick={(e) => {
          e.stopPropagation();
          onSelect(node.id);
        }}
        onPointerOver={(e) => {
          e.stopPropagation();
          setHovered(true);
          onHover(node.id);
          document.body.style.cursor = "none";
        }}
        onPointerOut={() => {
          setHovered(false);
          onHover(null);
        }}
      >
        <meshStandardMaterial
          color={colorHex}
          emissive={colorHex}
          emissiveIntensity={0.06}
          roughness={0.42}
          metalness={theme === "light" ? 0.22 : 0.34}
          transparent
          opacity={opacity}
          toneMapped
        />
        {showEdges && <Edges threshold={18} color={edge} scale={1.002} />}
      </mesh>

      {quality !== "low" && (
        <mesh ref={rail} geometry={RAIL_BOX} raycast={() => undefined}>
          <meshBasicMaterial color={COL_IN} transparent opacity={0.7} toneMapped={false} />
        </mesh>
      )}

      <mesh ref={pedestal} geometry={PEDESTAL_BOX} raycast={() => undefined} receiveShadow={false}>
        <meshStandardMaterial
          color={palette.pedestal}
          metalness={0.25}
          roughness={0.65}
          transparent
          opacity={opacity}
        />
      </mesh>

      {(active || hovered) && (
        <Html position={[0, height + 0.45, 0]} center distanceFactor={11} style={{ pointerEvents: "none" }}>
          <div className="whitespace-nowrap border border-hairline-strong bg-ink/95 px-2 py-1 font-mono text-[10px] tracking-[0.1em] text-paper uppercase shadow-sm backdrop-blur-sm">
            {node.id}
            <span className="ml-2 text-mute">{node.score.toFixed(2)}</span>
          </div>
        </Html>
      )}
    </group>
  );
}

function Links({
  nodes,
  stage,
  maxNpv,
  highlightIds,
  reduce,
  quality,
}: {
  nodes: SimNode[];
  stage: SimStage;
  maxNpv: number;
  highlightIds: Set<string>;
  reduce: boolean;
  quality: Quality;
}) {
  const selected = nodes.filter((n) => n.selected);
  const selectedCount = Math.max(selected.length, 1);
  const byId = useMemo(() => new Map(nodes.map((n) => [n.id, n])), [nodes]);

  const links = useMemo(() => {
    const out: { key: string; from: SimNode; to: SimNode }[] = [];
    nodes.forEach((n) => {
      n.dependsOn.forEach((dep) => {
        const parent = byId.get(dep);
        if (parent) out.push({ key: `${dep}-${n.id}`, from: parent, to: n });
      });
    });
    return out;
  }, [nodes, byId]);

  const prepared = useMemo(() => {
    return links.map((link) => {
      const fi = nodes.findIndex((n) => n.id === link.from.id);
      const ti = nodes.findIndex((n) => n.id === link.to.id);
      const fSel = Math.max(0, selected.findIndex((n) => n.id === link.from.id));
      const tSel = Math.max(0, selected.findIndex((n) => n.id === link.to.id));
      const aPos = positionForStage(link.from, stage, fi, nodes.length, fSel, selectedCount, maxNpv);
      const bPos = positionForStage(link.to, stage, ti, nodes.length, tSel, selectedCount, maxNpv);
      const hA = 0.5 + (link.from.budget / BUDGET_CAP) * 5.2;
      const hB = 0.5 + (link.to.budget / BUDGET_CAP) * 5.2;
      const hot = highlightIds.has(link.from.id) || highlightIds.has(link.to.id);
      const dim =
        (stage === "final" || stage === "done") && (!link.from.selected || !link.to.selected);
      const flowing = !dim && (stage === "dependencies" || hot);
      return {
        key: link.key,
        a: new THREE.Vector3(aPos[0], hA * 0.55, aPos[2]),
        b: new THREE.Vector3(bPos[0], hB * 0.55, bPos[2]),
        hot,
        dim,
        flowing,
        color: hot
          ? COL_IN
          : dim
            ? COL_OUT
            : stage === "dependencies"
              ? COL_WARN
              : COL_LINK,
      };
    });
  }, [links, nodes, selected, selectedCount, stage, maxNpv, highlightIds]);

  if (quality === "low" && stage !== "dependencies") {
    return (
      <>
        {prepared
          .filter((l) => l.hot)
          .map((link) => (
            <FlowLink
              key={link.key}
              a={link.a}
              b={link.b}
              color={link.color}
              lineWidth={1.6}
              opacity={0.75}
              flowing={false}
              reduce={reduce}
            />
          ))}
      </>
    );
  }

  return (
    <>
      {prepared.map((link) => (
        <FlowLink
          key={link.key}
          a={link.a}
          b={link.b}
          color={link.color}
          lineWidth={link.hot ? 2 : link.flowing ? 1.25 : 0.9}
          opacity={link.dim ? 0.05 : link.hot ? 0.85 : link.flowing ? 0.55 : 0.16}
          flowing={link.flowing && !reduce && quality !== "low"}
          reduce={reduce}
        />
      ))}
    </>
  );
}

function CameraRig({
  focusId,
  nodes,
  stage,
  maxNpv,
  focusToken,
}: {
  focusId: string | null;
  nodes: SimNode[];
  stage: SimStage;
  maxNpv: number;
  focusToken: number;
}) {
  const { camera, controls } = useThree();
  const selected = nodes.filter((n) => n.selected);
  const selectedCount = Math.max(selected.length, 1);

  useEffect(() => {
    if (!focusId) return;
    const index = nodes.findIndex((n) => n.id === focusId);
    if (index < 0) return;
    const node = nodes[index];
    const selIndex = Math.max(0, selected.findIndex((n) => n.id === focusId));
    const [x, , z] = positionForStage(
      node,
      stage,
      index,
      nodes.length,
      selIndex,
      selectedCount,
      maxNpv,
    );
    const h = 0.5 + (node.budget / BUDGET_CAP) * 5.2;
    const target = new THREE.Vector3(x, h * 0.45, z);
    const camPos = new THREE.Vector3(x + 4.2, h * 0.7 + 2.4, z + 4.8);
    const duration = 1.15;

    gsap.to(camera.position, {
      x: camPos.x,
      y: camPos.y,
      z: camPos.z,
      duration,
      ease: "power3.inOut",
    });
    const orbit = controls as unknown as { target: THREE.Vector3 } | null;
    if (orbit?.target) {
      gsap.to(orbit.target, {
        x: target.x,
        y: target.y,
        z: target.z,
        duration,
        ease: "power3.inOut",
      });
    }
  }, [focusId, focusToken, camera, controls, nodes, stage, selected, selectedCount, maxNpv]);

  useEffect(() => {
    if (focusId) return;
    if (
      stage !== "final" &&
      stage !== "done" &&
      stage !== "normalize" &&
      stage !== "knapsack" &&
      stage !== "dependencies"
    ) {
      return;
    }

    const overview =
      stage === "normalize"
        ? new THREE.Vector3(8.5, 6.2, 10)
        : stage === "knapsack"
          ? new THREE.Vector3(9.2, 5.8, 9.4)
          : stage === "dependencies"
            ? new THREE.Vector3(8.0, 5.6, 9.0)
            : new THREE.Vector3(7.2, 5.4, 8.5);

    gsap.to(camera.position, {
      x: overview.x,
      y: overview.y,
      z: overview.z,
      duration: 1.4,
      ease: "power2.inOut",
    });
  }, [stage, focusId, camera]);

  return null;
}

function SceneEffects({
  theme,
  reduce,
  quality,
}: {
  theme: Theme;
  reduce: boolean;
  quality: Quality;
}) {
  const palette = scenePalette(theme);

  if (quality === "low" || reduce) {
    return (
      <EffectComposer multisampling={0} enableNormalPass={false}>
        <Vignette
          offset={0.22}
          darkness={palette.vignette * 0.65}
          eskil={false}
          blendFunction={BlendFunction.NORMAL}
        />
        <ToneMapping mode={ToneMappingMode.ACES_FILMIC} />
      </EffectComposer>
    );
  }

  return (
    <EffectComposer multisampling={0} enableNormalPass={false}>
      {quality === "high" && (
        <N8AO
          aoRadius={0.9}
          intensity={theme === "light" ? 1.4 : 1.9}
          distanceFalloff={1.2}
          quality="performance"
          color={palette.aoColor}
          halfRes
        />
      )}
      <Bloom
        luminanceThreshold={0.78}
        luminanceSmoothing={0.4}
        intensity={quality === "high" ? palette.bloom : palette.bloom * 0.55}
        mipmapBlur={quality === "high"}
        radius={0.45}
      />
      <Vignette
        offset={0.22}
        darkness={palette.vignette}
        eskil={false}
        blendFunction={BlendFunction.NORMAL}
      />
      <ToneMapping mode={ToneMappingMode.ACES_FILMIC} />
    </EffectComposer>
  );
}

function SceneLighting({ theme, quality }: { theme: Theme; quality: Quality }) {
  const palette = scenePalette(theme);
  const keyRef = useRef<THREE.DirectionalLight>(null);
  const shadows = quality === "high";

  useEffect(() => {
    const light = keyRef.current;
    if (!light || !shadows) return;
    light.shadow.mapSize.set(512, 512);
    light.shadow.camera.near = 1;
    light.shadow.camera.far = 36;
    light.shadow.camera.left = -12;
    light.shadow.camera.right = 12;
    light.shadow.camera.top = 12;
    light.shadow.camera.bottom = -12;
    light.shadow.bias = -0.0003;
    light.shadow.normalBias = 0.035;
  }, [theme, shadows]);

  return (
    <>
      <color attach="background" args={[palette.bg]} />
      <fog attach="fog" args={[palette.bg, 14, 36]} />
      <hemisphereLight args={[palette.hemiSky, palette.hemiGround, palette.hemi]} />
      <ambientLight intensity={palette.ambient + (shadows ? 0 : 0.12)} />
      <directionalLight
        ref={keyRef}
        position={[7.5, 13, 5.5]}
        intensity={palette.key}
        color={theme === "light" ? "#ffffff" : "#faf9f5"}
        castShadow={shadows}
      />
      <directionalLight position={[-6.5, 3.5, -5]} intensity={palette.fill} color={palette.rim} />
      {quality !== "low" && (
        <pointLight
          position={[0, 6.5, 0]}
          intensity={0.26}
          color={COL_IN}
          distance={22}
          decay={2}
        />
      )}
    </>
  );
}

function SceneInner({
  nodes,
  stage,
  selectedId,
  onSelect,
  focusToken,
  theme,
  reduce,
  quality,
  lite,
}: {
  nodes: SimNode[];
  stage: SimStage;
  selectedId: string | null;
  onSelect: (id: string | null) => void;
  focusToken: number;
  theme: Theme;
  reduce: boolean;
  quality: Quality;
  lite: boolean;
}) {
  const [hoverId, setHoverId] = useState<string | null>(null);
  const maxNpv = Math.max(...nodes.map((n) => n.npv), 1);
  const selected = nodes.filter((n) => n.selected);
  const selectedCount = Math.max(selected.length, 1);
  const focus = selectedId ?? hoverId;
  const dustCount = quality === "high" ? 48 : quality === "medium" ? 24 : 0;

  const highlightIds = useMemo(() => {
    const set = new Set<string>();
    if (!focus) return set;
    set.add(focus);
    const node = nodes.find((n) => n.id === focus);
    node?.dependsOn.forEach((d) => set.add(d));
    nodes.forEach((n) => {
      if (n.dependsOn.includes(focus)) set.add(n.id);
    });
    return set;
  }, [focus, nodes]);

  return (
    <>
      <SceneLighting theme={theme} quality={quality} />
      <Floor theme={theme} quality={quality} />
      {dustCount > 0 && !lite && (
        <Dust count={dustCount} theme={theme} reduce={reduce} />
      )}
      <Links
        nodes={nodes}
        stage={stage}
        maxNpv={maxNpv}
        highlightIds={highlightIds}
        reduce={reduce}
        quality={quality}
      />

      {nodes.map((node, index) => {
        const selectedIndex = Math.max(0, selected.findIndex((n) => n.id === node.id));
        return (
          <ProjectTower
            key={node.id}
            node={node}
            stage={stage}
            index={index}
            total={nodes.length}
            selectedIndex={selectedIndex}
            selectedCount={selectedCount}
            maxNpv={maxNpv}
            active={selectedId === node.id}
            depHighlight={highlightIds.has(node.id) && selectedId !== node.id}
            theme={theme}
            reduce={reduce}
            quality={quality}
            onSelect={(id) => onSelect(id === selectedId ? null : id)}
            onHover={setHoverId}
          />
        );
      })}

      {!lite && (
        <CameraRig
          focusId={selectedId}
          nodes={nodes}
          stage={stage}
          maxNpv={maxNpv}
          focusToken={focusToken}
        />
      )}

      <OrbitControls
        makeDefault
        enableDamping
        dampingFactor={0.06}
        minDistance={5}
        maxDistance={28}
        maxPolarAngle={Math.PI * 0.48}
        minPolarAngle={0.18}
        target={[0, 1.4, 0]}
        enablePan={!lite}
        regress
      />

      <SceneEffects theme={theme} reduce={reduce} quality={quality} />
    </>
  );
}

export function PortfolioScene({
  nodes,
  stage,
  selectedId,
  onSelect,
  focusToken = 0,
  className,
  lite = false,
}: Props) {
  const { theme } = useTheme();
  const reduce = usePrefersReducedMotion();
  const hostRef = useRef<HTMLDivElement>(null);
  const visible = useCanvasVisible(hostRef);
  const [dpr, setDpr] = useState<[number, number]>(lite ? [1, 1.1] : [1, 1.35]);
  const [quality, setQuality] = useState<Quality>(lite || reduce ? "low" : "medium");

  useEffect(() => {
    if (lite || reduce) {
      setQuality("low");
      setDpr([1, 1.05]);
    }
  }, [lite, reduce]);

  return (
    <div
      ref={hostRef}
      className={cn("relative h-full min-h-[420px]", className)}
      style={{ background: scenePalette(theme).bg }}
    >
      <Canvas
        key={theme}
        dpr={dpr}
        shadows={quality === "high"}
        frameloop={visible ? "always" : "never"}
        camera={{ position: [9, 6.5, 11], fov: 38 }}
        gl={{
          antialias: quality !== "low",
          powerPreference: "high-performance",
          toneMapping: THREE.NoToneMapping,
          outputColorSpace: THREE.SRGBColorSpace,
          stencil: false,
          depth: true,
        }}
        performance={{ min: 0.4, max: 1, debounce: 280 }}
        onPointerMissed={() => onSelect(null)}
      >
        {!lite && visible && (
          <PerformanceMonitor
            bounds={(fps) => (fps > 90 ? [48, 70] : [32, 48])}
            onIncline={() => {
              if (reduce) return;
              setDpr([1, 1.35]);
              setQuality((q) => (q === "low" ? "medium" : "high"));
            }}
            onDecline={() => {
              setDpr([1, 1.05]);
              setQuality((q) => (q === "high" ? "medium" : "low"));
            }}
          />
        )}
        <Suspense fallback={null}>
          <SceneInner
            nodes={nodes}
            stage={stage}
            selectedId={selectedId}
            onSelect={onSelect}
            focusToken={focusToken}
            theme={theme}
            reduce={reduce}
            quality={quality}
            lite={lite}
          />
        </Suspense>
      </Canvas>
    </div>
  );
}
