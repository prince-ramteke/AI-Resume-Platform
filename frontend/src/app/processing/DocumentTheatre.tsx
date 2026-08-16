import { useRef } from "react";
import { DocumentPaper } from "../pipeline/DocumentPaper";
import { Atmosphere } from "./Atmosphere";
import { HighlightField } from "./HighlightField";
import { EvidenceRails } from "./EvidenceRails";
import { useEvidenceGeometry } from "./useEvidenceGeometry";

interface Props {
  stageIdx: number;
  analysisId?: string | number;
}

/**
 * The 2.5D document theatre — the processing page's signature visual.
 * Six explicit layers from back to front:
 *   L1 Atmosphere — radial gradient wash
 *   L2 Document shadow — implicit via paper elevation
 *   L3 Paper — DocumentPaper with gentle breathing transform
 *   L4 Scan beam — via DocumentPaper's `scanning` prop
 *   L5 Highlights — amber bars, staggered fade after Chunking
 *   L6 Evidence rails — cyan SVG curves toward pipeline tiles
 */
export function DocumentTheatre({ stageIdx, analysisId = 0 }: Props) {
  const theatreRef = useRef<HTMLDivElement | null>(null);

  const showBeam = stageIdx >= 1 && stageIdx <= 4;
  const beamDim = stageIdx >= 5;
  const showHighlights = stageIdx >= 3;
  const showRails = stageIdx >= 5;

  const rails = useEvidenceGeometry(theatreRef, showHighlights ? 5 : 0, showRails);

  return (
    <div ref={theatreRef} className="theatre" aria-hidden="true">
      <Atmosphere />
      <div className="theatre-paper-wrap">
        <DocumentPaper
          width={420}
          lines={18}
          highlights={showHighlights ? [2, 5, 8, 12, 15] : []}
          scanning={showBeam}
          className={`theatre-paper ${beamDim ? "theatre-paper--beam-dim" : ""}`}
        />
        {showHighlights && (
          <HighlightField
            analysisId={analysisId}
            count={5}
            visible={showHighlights}
          />
        )}
      </div>
      <EvidenceRails rails={rails} visible={showRails} />
    </div>
  );
}
